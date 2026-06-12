package com.david.worldcup.elo;

import com.david.worldcup.model.Match;

import java.time.LocalDate;
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

    private static boolean isEvaluated(Match match, LocalDate evaluateFrom) {
        return match.isWorldCupFinals() && !match.date().isBefore(evaluateFrom);
    }
}
