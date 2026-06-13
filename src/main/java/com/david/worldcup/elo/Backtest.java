package com.david.worldcup.elo;

import com.david.worldcup.model.Match;

import java.time.LocalDate;
import java.util.Locale;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Evaluates the Elo model on historical World Cups it has never "seen".
 *
 * <p>How it works: replay all matches in date order. Matches before
 * {@code evaluateFrom} are pure training. Inside the evaluation window
 * [{@code evaluateFrom}, {@code evaluateUntil}], every World Cup finals match
 * is <em>predicted first, then learned from</em> — the same information regime
 * the model would have faced in real time. Non-finals matches inside the window
 * (friendlies, other competitions) are still learned from but never scored.
 *
 * <p>Scoring:
 * <ul>
 *   <li><b>Accuracy</b> — the model's favorite is whichever side has expected
 *       score &ge; 0.5. A draw counts as a miss (the model never predicts draws —
 *       see the draw-modelling TODO in {@link EloRatingSystem}).</li>
 *   <li><b>Brier score</b> — mean of {@code (predicted − actual)²} with actual
 *       1 / 0.5 / 0. Punishes overconfidence; 0.25 is coin-flip level.</li>
 * </ul>
 */
public final class Backtest {

    /** A named evaluation window (one World Cup). */
    public record Window(String label, LocalDate from, LocalDate until) {}

    /** The five most recent completed World Cups. */
    public static final List<Window> WORLD_CUPS = List.of(
            new Window("World Cup 2006", LocalDate.of(2006, 6, 1), LocalDate.of(2006, 7, 31)),
            new Window("World Cup 2010", LocalDate.of(2010, 6, 1), LocalDate.of(2010, 7, 31)),
            new Window("World Cup 2014", LocalDate.of(2014, 6, 1), LocalDate.of(2014, 7, 31)),
            new Window("World Cup 2018", LocalDate.of(2018, 6, 1), LocalDate.of(2018, 7, 31)),
            new Window("World Cup 2022", LocalDate.of(2022, 11, 1), LocalDate.of(2022, 12, 31)));

    /** Windows used for tuning; 2022 is reserved for held-out validation. */
    public static final List<Window> TUNING_WINDOWS = WORLD_CUPS.subList(0, 4);

    /** Pools several windows into one match-weighted result. */
    public BacktestResult runCombined(List<Match> matches, List<Window> windows,
                                      EloConfig config) {
        int evaluated = 0;
        int correct = 0;
        double brierSum = 0.0;
        for (Window w : windows) {
            BacktestResult r = run(matches, w.from(), w.until(), config);
            evaluated += r.matchesEvaluated();
            correct += r.correctPredictions();
            brierSum += r.brierScore() * r.matchesEvaluated();
        }
        return new BacktestResult(evaluated, correct,
                evaluated == 0 ? 0.0 : (double) correct / evaluated,
                evaluated == 0 ? 0.0 : brierSum / evaluated);
    }

    public BacktestResult run(List<Match> matches, LocalDate evaluateFrom, LocalDate evaluateUntil) {
        return run(matches, evaluateFrom, evaluateUntil, EloConfig.DEFAULT);
    }

    public BacktestResult run(List<Match> matches, LocalDate evaluateFrom,
                              LocalDate evaluateUntil, EloConfig config) {
        List<Match> ordered = new ArrayList<>(matches);
        ordered.sort(Comparator.comparing(Match::date));

        EloRatingSystem elo = new EloRatingSystem(config);
        int evaluated = 0;
        int correct = 0;
        double brierSum = 0.0;

        for (Match match : ordered) {
            if (match.date().isAfter(evaluateUntil)) {
                break; // sorted, so nothing after this matters
            }
            if (isEvaluated(match, evaluateFrom)) {
                double predicted = elo.winProbability(
                        match.homeTeam(), match.awayTeam(), match.neutralVenue());

                double actual = switch (match.outcome()) {
                    case HOME_WIN -> 1.0;
                    case DRAW -> 0.5;
                    case AWAY_WIN -> 0.0;
                };
                brierSum += (predicted - actual) * (predicted - actual);

                Match.Outcome favored = predicted >= 0.5
                        ? Match.Outcome.HOME_WIN
                        : Match.Outcome.AWAY_WIN;
                if (match.outcome() == favored) {
                    correct++;
                }
                evaluated++;
            }
            // Predict BEFORE learning: only now does the model see the result.
            elo.processMatch(match);
        }

        return new BacktestResult(
                evaluated,
                correct,
                evaluated == 0 ? 0.0 : (double) correct / evaluated,
                evaluated == 0 ? 0.0 : brierSum / evaluated);
    }

    /**
     * Result of a three-way (win/draw/loss) backtest using the draw model.
     *
     * @param multiclassBrier mean of sum((p_i - actual_i)^2) over the three
     *                        outcomes; 0.667 = always predicting uniform thirds
     */
    public record ThreeWayResult(
            int matchesEvaluated,
            int correct,
            double accuracy,
            double multiclassBrier,
            int drawsPredicted,
            int actualDraws) {

        public String summary() {
            return String.format(Locale.ROOT,
                    "%d/%d correct (%.1f%%), multiclass Brier %.4f "
                            + "(predicted %d draws; %d actually occurred)",
                    correct, matchesEvaluated, accuracy * 100, multiclassBrier,
                    drawsPredicted, actualDraws);
        }
    }

    /**
     * Like {@link #run}, but predicts one of three outcomes (home/draw/away)
     * via the draw model and scores with the multi-class Brier score.
     */
    public ThreeWayResult runThreeWay(List<Match> matches, LocalDate evaluateFrom,
                                      LocalDate evaluateUntil, EloConfig config) {
        List<Match> ordered = new ArrayList<>(matches);
        ordered.sort(Comparator.comparing(Match::date));

        EloRatingSystem elo = new EloRatingSystem(config);
        int evaluated = 0;
        int correct = 0;
        int drawsPredicted = 0;
        int actualDraws = 0;
        double brierSum = 0.0;

        for (Match match : ordered) {
            if (match.date().isAfter(evaluateUntil)) {
                break;
            }
            if (isEvaluated(match, evaluateFrom)) {
                DrawModel.Probabilities p = elo.outcomeProbabilities(
                        match.homeTeam(), match.awayTeam(), match.neutralVenue());
                double[] probs = {p.homeWin(), p.draw(), p.awayWin()};

                int actual = switch (match.outcome()) {
                    case HOME_WIN -> 0;
                    case DRAW -> 1;
                    case AWAY_WIN -> 2;
                };
                for (int i = 0; i < 3; i++) {
                    double target = i == actual ? 1.0 : 0.0;
                    brierSum += (probs[i] - target) * (probs[i] - target);
                }

                int predicted = 0;
                if (probs[1] > probs[predicted]) predicted = 1;
                if (probs[2] > probs[predicted]) predicted = 2;

                if (predicted == actual) correct++;
                if (predicted == 1) drawsPredicted++;
                if (actual == 1) actualDraws++;
                evaluated++;
            }
            elo.processMatch(match);
        }

        return new ThreeWayResult(
                evaluated,
                correct,
                evaluated == 0 ? 0.0 : (double) correct / evaluated,
                evaluated == 0 ? 0.0 : brierSum / evaluated,
                drawsPredicted,
                actualDraws);
    }

    private static boolean isEvaluated(Match match, LocalDate evaluateFrom) {
        return match.isWorldCupFinals() && !match.date().isBefore(evaluateFrom);
    }
}
