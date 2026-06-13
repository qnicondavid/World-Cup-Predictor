package com.david.worldcup.tracker;

import com.david.worldcup.elo.DrawModel;
import com.david.worldcup.elo.EloRatingSystem;
import com.david.worldcup.goals.ScorePredictor;
import com.david.worldcup.model.Fixture;
import com.david.worldcup.model.Match;
import com.david.worldcup.sim.TournamentSimulator;
import com.david.worldcup.tracker.PredictionLedger.Prediction;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * The live 2026 tracker: locks predictions for upcoming World Cup fixtures and
 * scores previously locked predictions against completed results.
 *
 * <p>Predictions are locked and scored three-way (win/draw/loss) using the
 * {@link DrawModel}. A "hit" means the model's single most likely outcome
 * matched the actual result; the Brier score is the multiclass Brier over the
 * three outcomes (0 = perfect, 2 = worst; a uniform 1/3-1/3-1/3 guess = 0.667).
 */
public final class Tracker {

    public static final String SECTION_START = "<!-- TRACKER:START -->";
    public static final String SECTION_END = "<!-- TRACKER:END -->";
    public static final String TITLE_SECTION_START = "<!-- TITLE:START -->";
    public static final String TITLE_SECTION_END = "<!-- TITLE:END -->";

    private static final DateTimeFormatter DAY =
            DateTimeFormatter.ofPattern("MMM d", Locale.ENGLISH);

    public record ScoredPrediction(Prediction prediction, Match result,
                                   boolean correct, double brier) {}

    /** Predictions for World Cup fixtures on/after {@code today} that are not in the ledger yet. */
    public static List<Prediction> lockNewPredictions(EloRatingSystem elo,
                                                      List<Fixture> fixtures,
                                                      List<Prediction> ledger,
                                                      LocalDate today) {
        Set<String> alreadyLocked = new HashSet<>();
        for (Prediction p : ledger) {
            alreadyLocked.add(key(p.matchDate(), p.homeTeam(), p.awayTeam()));
        }

        return fixtures.stream()
                .filter(Fixture::isWorldCupFinals)
                .filter(fx -> !fx.date().isBefore(today))
                .filter(fx -> !alreadyLocked.contains(key(fx.date(), fx.homeTeam(), fx.awayTeam())))
                .map(fx -> {
                    DrawModel.Probabilities p = elo.outcomeProbabilities(
                            fx.homeTeam(), fx.awayTeam(), fx.neutralVenue());
                    return new Prediction(fx.date(), fx.homeTeam(), fx.awayTeam(),
                            fx.neutralVenue(), p.homeWin(), p.draw(), p.awayWin(), today);
                })
                .sorted(Comparator.comparing(Prediction::matchDate))
                .toList();
    }

    /** Matches each ledger entry against completed results; unplayed entries are omitted. */
    public static List<ScoredPrediction> score(List<Prediction> ledger, List<Match> completed) {
        Map<String, Match> results = new HashMap<>();
        for (Match m : completed) {
            results.putIfAbsent(key(m.date(), m.homeTeam(), m.awayTeam()), m);
        }

        List<ScoredPrediction> scored = new ArrayList<>();
        for (Prediction p : ledger) {
            Match result = results.get(key(p.matchDate(), p.homeTeam(), p.awayTeam()));
            if (result == null) {
                continue;
            }
            Match.Outcome actual = result.outcome();
            scored.add(new ScoredPrediction(p, result,
                    p.predictedOutcome() == actual,
                    multiclassBrier(p, actual)));
        }
        return scored;
    }

    /**
     * Multiclass Brier score for one prediction: the squared error summed over
     * the win, draw and loss probabilities against the one-hot actual outcome.
     */
    static double multiclassBrier(Prediction p, Match.Outcome actual) {
        return sq(p.pHome() - indicator(actual, Match.Outcome.HOME_WIN))
                + sq(p.pDraw() - indicator(actual, Match.Outcome.DRAW))
                + sq(p.pAway() - indicator(actual, Match.Outcome.AWAY_WIN));
    }

    private static double indicator(Match.Outcome actual, Match.Outcome target) {
        return actual == target ? 1.0 : 0.0;
    }

    private static double sq(double x) {
        return x * x;
    }

    /** Renders the README tracker section as Markdown. */
    public static String renderMarkdown(List<ScoredPrediction> scored,
                                        List<Prediction> pending,
                                        LocalDate today) {
        StringBuilder md = new StringBuilder();
        md.append("_Updated ").append(today)
          .append(" — predictions are locked before kickoff and never edited; ")
          .append("the git history of `predictions/predictions.csv` is the proof. ")
          .append("Each pick is the model's most likely outcome and the H/D/A column its ")
          .append("full home-win / draw / away-win split; the predicted score is the most ")
          .append("likely scoreline (expected goals in brackets), and Δ is the total goal ")
          .append("difference from the actual result (🎯 = exact). Brier is multiclass._\n\n");

        if (scored.isEmpty()) {
            md.append("**No locked predictions have been resolved yet.**\n");
        } else {
            long correct = scored.stream().filter(ScoredPrediction::correct).count();
            double brier = scored.stream().mapToDouble(ScoredPrediction::brier).average().orElse(0);
            double meanGoalError = scored.stream()
                    .mapToInt(s -> predictedScore(s.prediction())
                            .goalError(s.result().homeScore(), s.result().awayScore()))
                    .average().orElse(0);
            md.append(String.format(Locale.ROOT,
                    "**Record: %d/%d picks correct (%.1f%%) — multiclass Brier %.3f — "
                            + "mean goal error %.1f** (uniform guess = 0.667)%n%n",
                    correct, scored.size(), 100.0 * correct / scored.size(), brier, meanGoalError));

            md.append("| Date | Match | Pick | H/D/A | Pred (xG) | Result | Δ | Hit |\n");
            md.append("|---|---|---|---|---|---|---|---|\n");
            List<ScoredPrediction> recent =
                    scored.subList(Math.max(0, scored.size() - 15), scored.size());
            for (ScoredPrediction s : recent) {
                Prediction p = s.prediction();
                Match r = s.result();
                ScorePredictor.PredictedScore ps = predictedScore(p);
                int err = ps.goalError(r.homeScore(), r.awayScore());
                String delta = err + (ps.exact(r.homeScore(), r.awayScore()) ? " 🎯" : "");
                md.append(String.format(Locale.ROOT,
                        "| %s | %s vs %s | %s | %s | %s | %d-%d | %s | %s |%n",
                        DAY.format(p.matchDate()), p.homeTeam(), p.awayTeam(),
                        p.pick(), splitLabel(p), scoreLabel(ps),
                        r.homeScore(), r.awayScore(),
                        delta, s.correct() ? "✅" : "❌"));
            }
        }

        if (!pending.isEmpty()) {
            md.append("\n**Locked for upcoming matches:**\n\n");
            md.append("| Date | Match | Pick | H/D/A | Pred (xG) |\n");
            md.append("|---|---|---|---|---|\n");
            pending.stream()
                    .sorted(Comparator.comparing(Prediction::matchDate))
                    .limit(10)
                    .forEach(p -> md.append(String.format(Locale.ROOT,
                            "| %s | %s vs %s | %s | %s | %s |%n",
                            DAY.format(p.matchDate()), p.homeTeam(), p.awayTeam(),
                            p.pick(), splitLabel(p), scoreLabel(predictedScore(p)))));
        }
        return md.toString();
    }

    /** Compact "73/18/9%" home-win / draw / away-win label. */
    private static String splitLabel(Prediction p) {
        return String.format(Locale.ROOT, "%.0f/%.0f/%.0f%%",
                100 * p.pHome(), 100 * p.pDraw(), 100 * p.pAway());
    }

    /** Predicted scoreline, derived from the same Elo gap that produced the pick. */
    private static ScorePredictor.PredictedScore predictedScore(Prediction p) {
        return ScorePredictor.fromExpectedScore(p.pHome() + p.pDraw() / 2.0);
    }

    /** "2-1 (1.9–0.8)" — most likely scoreline with expected goals in brackets. */
    private static String scoreLabel(ScorePredictor.PredictedScore ps) {
        return String.format(Locale.ROOT, "%d-%d (%.1f–%.1f)",
                ps.modalHome(), ps.modalAway(), ps.homeGoals(), ps.awayGoals());
    }

    /**
     * Renders the live championship-odds table from the Monte Carlo simulator.
     *
     * @param topN number of teams to list, best title odds first
     * @param runs number of simulations behind the odds (for the caption)
     */
    public static String renderTitleOdds(List<TournamentSimulator.TeamOdds> odds,
                                         int topN, LocalDate today, int runs) {
        StringBuilder md = new StringBuilder();
        md.append(String.format(Locale.ROOT,
                "_The model's championship odds from %,d Monte Carlo simulations, updated %s. "
                        + "They inherit the simulator's simplifications (Elo tie-breaks, seeded "
                        + "knockout pairings, knockout games as neutral with no draws), so read "
                        + "them as the model's view, not a hard forecast._%n%n", runs, today));
        md.append("| # | Team | Title | Final | Semis |\n");
        md.append("|---|---|---|---|---|\n");
        int n = Math.min(topN, odds.size());
        for (int i = 0; i < n; i++) {
            TournamentSimulator.TeamOdds o = odds.get(i);
            md.append(String.format(Locale.ROOT, "| %d | %s | %.1f%% | %.1f%% | %.1f%% |%n",
                    i + 1, o.team(),
                    100 * o.titleShare(), 100 * o.finalShare(), 100 * o.semiShare()));
        }
        return md.toString();
    }

    /** Replaces the content between the tracker markers; appends a section if absent. */
    public static String replaceSection(String readme, String newSection) {
        return replaceSection(readme, SECTION_START, SECTION_END, newSection);
    }

    /** Replaces the content between the given markers; appends a section if absent. */
    public static String replaceSection(String readme, String startMarker, String endMarker,
                                        String newSection) {
        int start = readme.indexOf(startMarker);
        int end = readme.indexOf(endMarker);
        if (start < 0 || end < 0 || end < start) {
            return readme + "\n" + startMarker + "\n" + newSection + "\n" + endMarker + "\n";
        }
        return readme.substring(0, start + startMarker.length())
                + "\n" + newSection + "\n"
                + readme.substring(end);
    }

    private static String key(LocalDate date, String home, String away) {
        return date + "|" + home + "|" + away;
    }

    private Tracker() {
    }
}
