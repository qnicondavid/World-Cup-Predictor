package com.david.worldcup.tracker;

import com.david.worldcup.elo.EloRatingSystem;
import com.david.worldcup.model.Fixture;
import com.david.worldcup.model.Match;
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
 */
public final class Tracker {

    public static final String SECTION_START = "<!-- TRACKER:START -->";
    public static final String SECTION_END = "<!-- TRACKER:END -->";

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
                .map(fx -> new Prediction(
                        fx.date(), fx.homeTeam(), fx.awayTeam(), fx.neutralVenue(),
                        elo.winProbability(fx.homeTeam(), fx.awayTeam(), fx.neutralVenue()),
                        today))
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
            double actual = switch (result.outcome()) {
                case HOME_WIN -> 1.0;
                case DRAW -> 0.5;
                case AWAY_WIN -> 0.0;
            };
            Match.Outcome favored = p.pHome() >= 0.5
                    ? Match.Outcome.HOME_WIN
                    : Match.Outcome.AWAY_WIN;
            scored.add(new ScoredPrediction(p, result,
                    result.outcome() == favored,
                    (p.pHome() - actual) * (p.pHome() - actual)));
        }
        return scored;
    }

    /** Renders the README tracker section as Markdown. */
    public static String renderMarkdown(List<ScoredPrediction> scored,
                                        List<Prediction> pending,
                                        LocalDate today) {
        StringBuilder md = new StringBuilder();
        md.append("_Updated ").append(today)
          .append(" — predictions are locked before kickoff and never edited; ")
          .append("the git history of `predictions/predictions.csv` is the proof._\n\n");

        if (scored.isEmpty()) {
            md.append("**No locked predictions have been resolved yet.**\n");
        } else {
            long correct = scored.stream().filter(ScoredPrediction::correct).count();
            double brier = scored.stream().mapToDouble(ScoredPrediction::brier).average().orElse(0);
            md.append(String.format(Locale.ROOT,
                    "**Record: %d/%d correct (%.1f%%) — Brier %.3f** (coin flip = 0.250)%n%n",
                    correct, scored.size(), 100.0 * correct / scored.size(), brier));

            md.append("| Date | Match | Pick (locked) | Result | Hit |\n");
            md.append("|---|---|---|---|---|\n");
            List<ScoredPrediction> recent =
                    scored.subList(Math.max(0, scored.size() - 15), scored.size());
            for (ScoredPrediction s : recent) {
                Prediction p = s.prediction();
                Match r = s.result();
                md.append(String.format(Locale.ROOT,
                        "| %s | %s vs %s | %s (%.0f%%) | %d-%d | %s |%n",
                        DAY.format(p.matchDate()), p.homeTeam(), p.awayTeam(),
                        p.favorite(), 100 * p.favoriteProbability(),
                        r.homeScore(), r.awayScore(),
                        s.correct() ? "✅" : "❌"));
            }
        }

        if (!pending.isEmpty()) {
            md.append("\n**Locked for upcoming matches:**\n\n");
            md.append("| Date | Match | Pick | Confidence |\n");
            md.append("|---|---|---|---|\n");
            pending.stream()
                    .sorted(Comparator.comparing(Prediction::matchDate))
                    .limit(10)
                    .forEach(p -> md.append(String.format(Locale.ROOT,
                            "| %s | %s vs %s | %s | %.0f%% |%n",
                            DAY.format(p.matchDate()), p.homeTeam(), p.awayTeam(),
                            p.favorite(), 100 * p.favoriteProbability())));
        }
        return md.toString();
    }

    /** Replaces the content between the tracker markers; appends a section if absent. */
    public static String replaceSection(String readme, String newSection) {
        int start = readme.indexOf(SECTION_START);
        int end = readme.indexOf(SECTION_END);
        if (start < 0 || end < 0 || end < start) {
            return readme + "\n" + SECTION_START + "\n" + newSection + "\n" + SECTION_END + "\n";
        }
        return readme.substring(0, start + SECTION_START.length())
                + "\n" + newSection + "\n"
                + readme.substring(end);
    }

    private static String key(LocalDate date, String home, String away) {
        return date + "|" + home + "|" + away;
    }

    private Tracker() {
    }
}
