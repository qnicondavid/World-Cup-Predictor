package com.david.worldcup;

import com.david.worldcup.data.MatchCsvParser;
import com.david.worldcup.elo.Backtest;
import com.david.worldcup.elo.BacktestResult;
import com.david.worldcup.elo.EloConfig;
import com.david.worldcup.elo.EloRatingSystem;
import com.david.worldcup.elo.Tuner;
import com.david.worldcup.model.Match;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * CLI entry point.
 *
 * <ul>
 *   <li>{@code mvn compile exec:java} — replay history, print Elo top 15 + 2026 predictions</li>
 *   <li>{@code mvn compile exec:java -Dexec.args="--backtest"} — evaluate on 2018/2022,
 *       baseline vs goal-margin scaling</li>
 *   <li>{@code mvn compile exec:java -Dexec.args="--tune"} — hyperparameter grid search
 *       (tuned on 2018, validated once on 2022)</li>
 * </ul>
 */
public final class Main {

    public static void main(String[] args) throws IOException {
        List<String> arguments = Arrays.asList(args);
        Path csv = arguments.stream()
                .filter(a -> !a.startsWith("--"))
                .findFirst()
                .map(Path::of)
                .orElse(Path.of("data/results.csv"));

        List<Match> matches = new MatchCsvParser().parse(csv);
        matches.sort(Comparator.comparing(Match::date));

        if (arguments.contains("--backtest")) {
            runBacktests(matches);
        } else if (arguments.contains("--tune")) {
            runTuning(matches);
        } else {
            runRankings(matches);
        }
    }

    private static void runRankings(List<Match> matches) {
        EloRatingSystem elo = new EloRatingSystem();
        matches.forEach(elo::processMatch);

        System.out.printf("Processed %,d matches between %d teams (%s to %s)%n%n",
                elo.matchesProcessed(),
                elo.teamCount(),
                matches.get(0).date(),
                matches.get(matches.size() - 1).date());

        System.out.println("=== Elo Top 15 ===");
        int rank = 1;
        for (var entry : elo.topRatings(15)) {
            System.out.printf("%2d. %-20s %.0f%n", rank++, entry.getKey(), entry.getValue());
        }

        System.out.println();
        System.out.println("=== Sample 2026 group-stage predictions (neutral venue) ===");
        printPrediction(elo, "Panama", "England");
        printPrediction(elo, "Croatia", "Ghana");
        printPrediction(elo, "Brazil", "Morocco");
    }

    private static void runBacktests(List<Match> matches) {
        System.out.println("=== Backtest: model evaluated on World Cups it has never seen ===");
        System.out.println();
        Backtest backtest = new Backtest();

        printBacktest(backtest, matches, "Baseline (no margin scaling)", EloConfig.BASELINE);
        System.out.println();
        printBacktest(backtest, matches, "With goal-margin K scaling", EloConfig.DEFAULT);

        System.out.println();
        System.out.println("Reference points: coin flip = 50% accuracy, Brier 0.25.");
        System.out.println("Draws always count as misses, so accuracy is understated.");
    }

    private static void printBacktest(Backtest backtest, List<Match> matches,
                                      String label, EloConfig config) {
        BacktestResult wc2018 = backtest.run(matches,
                LocalDate.of(2018, 6, 1), LocalDate.of(2018, 7, 31), config);
        BacktestResult wc2022 = backtest.run(matches,
                LocalDate.of(2022, 11, 1), LocalDate.of(2022, 12, 31), config);
        System.out.println("--- " + label + " ---");
        System.out.println("World Cup 2018: " + wc2018.summary());
        System.out.println("World Cup 2022: " + wc2022.summary());
    }

    private static void runTuning(List<Match> matches) {
        System.out.println("=== Hyperparameter grid search ===");
        System.out.println("Tuning metric: Brier score on World Cup 2018 (lower is better).");
        System.out.println("World Cup 2022 is held out for final validation.");
        System.out.println();

        Tuner tuner = new Tuner();
        List<Tuner.Candidate> candidates = tuner.search(matches);

        System.out.printf("%-9s %-9s %-10s %-7s | %s%n",
                "kWC", "homeAdv", "kFriendly", "margin", "2018 tuning result");
        for (Tuner.Candidate c : candidates.subList(0, 5)) {
            EloConfig cfg = c.config();
            System.out.printf("%-9.0f %-9.0f %-10.0f %-7s | %s%n",
                    cfg.kWorldCup(), cfg.homeAdvantage(), cfg.kFriendly(),
                    cfg.goalMarginScaling() ? "on" : "off",
                    c.tuningResult().summary());
        }

        EloConfig best = candidates.get(0).config();
        System.out.println();
        System.out.println("Held-out validation of the winner on World Cup 2022:");
        System.out.println("  " + tuner.validate(matches, best).summary());
        System.out.println();
        System.out.println("Current EloConfig.DEFAULT on 2022 for comparison:");
        System.out.println("  " + tuner.validate(matches, EloConfig.DEFAULT).summary());
    }

    private static void printPrediction(EloRatingSystem elo, String teamA, String teamB) {
        double p = elo.winProbability(teamA, teamB, true);
        System.out.printf("%s (%.0f) vs %s (%.0f): %s expected to win with score %.2f%n",
                teamA, elo.ratingOf(teamA),
                teamB, elo.ratingOf(teamB),
                p >= 0.5 ? teamA : teamB,
                p >= 0.5 ? p : 1 - p);
    }

    private Main() {
    }
}
