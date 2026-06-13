package com.david.worldcup;

import com.david.worldcup.data.MatchCsvParser;
import com.david.worldcup.elo.Backtest;
import com.david.worldcup.elo.BacktestResult;
import com.david.worldcup.elo.EloConfig;
import com.david.worldcup.elo.EloRatingSystem;
import com.david.worldcup.elo.Tuner;
import com.david.worldcup.elo.DrawModel;
import com.david.worldcup.goals.BivariatePoissonModel;
import com.david.worldcup.goals.DixonColesModel;
import com.david.worldcup.goals.EloDrawBaselineModel;
import com.david.worldcup.goals.EloPoissonModel;
import com.david.worldcup.goals.EnsembleModel;
import com.david.worldcup.goals.GoalModel;
import com.david.worldcup.goals.GoalModelBacktest;
import com.david.worldcup.model.Fixture;
import com.david.worldcup.model.Match;
import com.david.worldcup.rest.RestBacktest;
import com.david.worldcup.sim.TournamentSimulator;
import com.david.worldcup.tracker.PredictionLedger;
import com.david.worldcup.tracker.Tracker;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * CLI entry point.
 *
 * <ul>
 *   <li>{@code mvn compile exec:java} — replay history, print Elo top 15 + 2026 predictions</li>
 *   <li>{@code -Dexec.args="--backtest"} — evaluate on 2018/2022, baseline vs margin scaling</li>
 *   <li>{@code -Dexec.args="--tune"} — hyperparameter grid search (tuned 2018, validated 2022)</li>
 *   <li>{@code -Dexec.args="--track"} — live tracker: lock predictions for upcoming World Cup
 *       fixtures, score completed ones, update the README accuracy table</li>
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
        } else if (arguments.contains("--track")) {
            runTracker(matches, csv);
        } else if (arguments.contains("--simulate")) {
            runSimulation(matches, csv);
        } else if (arguments.contains("--upcoming")) {
            runUpcoming(matches, csv);
        } else if (arguments.contains("--goals")) {
            runGoalComparison(matches);
        } else if (arguments.contains("--rest")) {
            runRest(matches);
        } else if (arguments.stream().anyMatch(a -> a.startsWith("--predict="))) {
            runPredict(matches, arguments);
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

        System.out.printf("%-16s | %-38s | %s%n", "Tournament", "Tuned model", "Baseline");
        for (Backtest.Window w : Backtest.WORLD_CUPS) {
            BacktestResult tuned = backtest.run(matches, w.from(), w.until(), EloConfig.DEFAULT);
            BacktestResult base = backtest.run(matches, w.from(), w.until(), EloConfig.BASELINE);
            System.out.printf("%-16s | %-38s | %s%n", w.label(), tuned.summary(), base.summary());
        }
        BacktestResult combinedTuned =
                backtest.runCombined(matches, Backtest.WORLD_CUPS, EloConfig.DEFAULT);
        BacktestResult combinedBase =
                backtest.runCombined(matches, Backtest.WORLD_CUPS, EloConfig.BASELINE);
        System.out.printf("%-16s | %-38s | %s%n", "Combined", combinedTuned.summary(),
                combinedBase.summary());

        System.out.println();
        System.out.println("--- Three-way (win/draw/loss) with the draw model, tuned config ---");
        for (Backtest.Window w : Backtest.WORLD_CUPS) {
            System.out.println(w.label() + ": "
                    + backtest.runThreeWay(matches, w.from(), w.until(), EloConfig.DEFAULT)
                            .summary());
        }
        System.out.println("Reference: predicting uniform thirds = multiclass Brier 0.667.");

        System.out.println();
        System.out.println("Reference points: coin flip = 50% accuracy, Brier 0.25.");
        System.out.println("Draws always count as misses in the binary rows, so accuracy is understated.");
    }

    private static void runGoalComparison(List<Match> matches) {
        System.out.println("=== Goal models vs Elo baseline: held-out World Cups (three-way) ===");
        System.out.println("Each model trains only on the 12 years before each tournament, then");
        System.out.println("predicts every finals match from that fit. Lower multiclass Brier is better.");
        System.out.println();

        record Entry(String name, GoalModelBacktest.Factory factory) {}
        List<Entry> models = List.of(
                new Entry("Dixon-Coles", (tr, asof) -> DixonColesModel.fit(tr, asof)),
                new Entry("Bivariate Poisson", (tr, asof) -> BivariatePoissonModel.fit(tr, asof)),
                new Entry("Elo-Poisson", (tr, asof) -> EloPoissonModel.fit(tr)),
                new Entry("Elo + DrawModel", (tr, asof) -> EloDrawBaselineModel.fit(tr)),
                new Entry("Elo+DC ensemble", (tr, asof) -> new EnsembleModel("Elo+DC ensemble",
                        List.of(DixonColesModel.fit(tr, asof), EloDrawBaselineModel.fit(tr)))));

        GoalModelBacktest bt = new GoalModelBacktest(12);
        System.out.printf("%-20s | %-13s | %s%n",
                "Model", "Combined", "per-tournament Brier (2006/10/14/18/22)");
        for (Entry e : models) {
            int evaluated = 0;
            int correct = 0;
            double brierSum = 0.0;
            StringBuilder per = new StringBuilder();
            for (Backtest.Window w : Backtest.WORLD_CUPS) {
                Backtest.ThreeWayResult r = bt.run(matches, w, e.factory());
                per.append(String.format(Locale.ROOT, " %.3f", r.multiclassBrier()));
                evaluated += r.matchesEvaluated();
                correct += r.correct();
                brierSum += r.multiclassBrier() * r.matchesEvaluated();
            }
            double combined = evaluated == 0 ? 0.0 : brierSum / evaluated;
            System.out.printf(Locale.ROOT, "%-20s | %3d/%-4d %.3f |%s%n",
                    e.name(), correct, evaluated, combined, per);
        }

        System.out.println();
        System.out.println("Reference: uniform thirds = multiclass Brier 0.667.");
    }

    private static void runRest(List<Match> matches) {
        System.out.println("=== Rest-days differential: does extra recovery beat the plain rating? ===");
        System.out.println("Adds rating points per day of rest advantage; 0 = Elo + DrawModel baseline.");
        System.out.println();

        RestBacktest bt = new RestBacktest();
        double[] coeffs = {0, 5, 10, 15, 20, 30};

        System.out.printf("%-9s | %s%n", "pts/day", "pooled WC 2006-2018 (three-way)");
        double bestCoeff = 0;
        double bestBrier = Double.MAX_VALUE;
        for (double c : coeffs) {
            int evaluated = 0;
            int correct = 0;
            double brierSum = 0.0;
            for (Backtest.Window w : Backtest.TUNING_WINDOWS) {
                RestBacktest.Result r = bt.run(matches, w.from(), w.until(), c);
                evaluated += r.evaluated();
                correct += r.correct();
                brierSum += r.multiclassBrier() * r.evaluated();
            }
            double brier = evaluated == 0 ? 0.0 : brierSum / evaluated;
            System.out.printf(Locale.ROOT, "%-9.0f | %d/%d correct, Brier %.4f%n",
                    c, correct, evaluated, brier);
            if (brier < bestBrier) {
                bestBrier = brier;
                bestCoeff = c;
            }
        }

        Backtest.Window validation = Backtest.WORLD_CUPS.get(4); // 2022, held out
        RestBacktest.Result base = bt.run(matches, validation.from(), validation.until(), 0);
        RestBacktest.Result tuned = bt.run(matches, validation.from(), validation.until(), bestCoeff);
        System.out.println();
        System.out.printf(Locale.ROOT,
                "Best on tuning: %.0f points per rest-day. Held-out World Cup 2022:%n", bestCoeff);
        System.out.println("  baseline (0):       " + base.summary());
        System.out.printf(Locale.ROOT, "  rest-adjusted (%.0f): %s%n", bestCoeff, tuned.summary());
        if (bestCoeff == 0) {
            System.out.println("Verdict: rest differential did not improve on the baseline.");
        }
    }

    private static void runTuning(List<Match> matches) {
        System.out.println("=== Hyperparameter grid search ===");
        System.out.println("Tuning metric: pooled Brier over WC 2006-2018 (256 matches).");
        System.out.println("World Cup 2022 is held out for final validation.");
        System.out.println();

        Tuner tuner = new Tuner();
        List<Tuner.Candidate> candidates = tuner.search(matches);

        System.out.printf("%-7s %-9s %-10s %-7s %-7s | %s%n",
                "kWC", "homeAdv", "kFriendly", "margin", "regr", "pooled 2006-2018 result");
        for (Tuner.Candidate c : candidates.subList(0, 8)) {
            EloConfig cfg = c.config();
            System.out.printf("%-7.0f %-9.0f %-10.0f %-7s %-7.2f | %s%n",
                    cfg.kWorldCup(), cfg.homeAdvantage(), cfg.kFriendly(),
                    cfg.goalMarginScaling() ? "on" : "off",
                    cfg.annualRegression(),
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

    private static void runTracker(List<Match> matches, Path csv) throws IOException {
        LocalDate today = LocalDate.now();
        Path ledgerPath = Path.of("predictions/predictions.csv");
        Path readmePath = Path.of("README.md");

        // Train on everything that has been played (Elo still drives the title-odds simulation).
        EloRatingSystem elo = new EloRatingSystem();
        matches.forEach(elo::processMatch);

        // Production prediction model: Dixon-Coles, the best performer in the held-out
        // comparison, fit on all history as of today.
        DixonColesModel predictionModel = DixonColesModel.fit(matches, today);

        // Lock predictions for upcoming World Cup fixtures not yet in the ledger.
        List<Fixture> fixtures = new MatchCsvParser().parseFixtures(csv);
        List<PredictionLedger.Prediction> ledger =
                new ArrayList<>(PredictionLedger.load(ledgerPath));
        List<PredictionLedger.Prediction> added =
                Tracker.lockNewPredictions(predictionModel, fixtures, ledger, today);
        ledger.addAll(added);
        ledger.sort(Comparator.comparing(PredictionLedger.Prediction::matchDate));
        PredictionLedger.save(ledgerPath, ledger);

        // Score everything in the ledger that now has a result.
        List<Tracker.ScoredPrediction> scored = Tracker.score(ledger, matches);
        List<PredictionLedger.Prediction> pending = new ArrayList<>(ledger);
        scored.forEach(s -> pending.remove(s.prediction()));

        // Rewrite the README prediction-accuracy section.
        String readme = Files.readString(readmePath);
        readme = Tracker.replaceSection(readme, Tracker.renderMarkdown(scored, pending, today));

        // Rewrite the live championship-odds section from a fresh simulation.
        List<Match> played2026 = matches.stream()
                .filter(Match::isWorldCupFinals)
                .filter(mt -> mt.date().getYear() == 2026)
                .toList();
        List<Fixture> remainingWorldCup = fixtures.stream()
                .filter(Fixture::isWorldCupFinals)
                .toList();
        int runs = 10_000;
        List<TournamentSimulator.TeamOdds> odds =
                new TournamentSimulator(elo).simulate(played2026, remainingWorldCup, runs, 2026L);
        readme = Tracker.replaceSection(readme,
                Tracker.TITLE_SECTION_START, Tracker.TITLE_SECTION_END,
                Tracker.renderTitleOdds(odds, 16, today, runs));

        // Retrospective view of World Cup matches played before the model was created.
        LocalDate modelBirth = ledger.stream()
                .map(PredictionLedger.Prediction::lockedOn)
                .min(Comparator.naturalOrder())
                .orElse(today);
        List<Match> earlyMatches = matches.stream()
                .filter(Match::isWorldCupFinals)
                .filter(mt -> mt.date().getYear() == 2026)
                .filter(mt -> mt.date().isBefore(modelBirth))
                .sorted(Comparator.comparing(Match::date))
                .toList();
        List<PredictionLedger.Prediction> earlyPredictions = new ArrayList<>();
        for (Match mt : earlyMatches) {
            List<Match> before = matches.stream().filter(x -> x.date().isBefore(mt.date())).toList();
            DixonColesModel retro = DixonColesModel.fit(before, mt.date());
            DrawModel.Probabilities pr =
                    retro.probabilities(mt.homeTeam(), mt.awayTeam(), mt.neutralVenue());
            var goals = retro.expectedGoals(mt.homeTeam(), mt.awayTeam(), mt.neutralVenue());
            earlyPredictions.add(new PredictionLedger.Prediction(
                    mt.date(), mt.homeTeam(), mt.awayTeam(), mt.neutralVenue(),
                    pr.homeWin(), pr.draw(), pr.awayWin(),
                    goals.map(GoalModel.GoalRates::home).orElse(Double.NaN),
                    goals.map(GoalModel.GoalRates::away).orElse(Double.NaN),
                    mt.date()));
        }
        readme = Tracker.replaceSection(readme,
                Tracker.EARLY_SECTION_START, Tracker.EARLY_SECTION_END,
                Tracker.renderEarlyMatches(Tracker.score(earlyPredictions, matches), today));

        Files.writeString(readmePath, readme);

        System.out.printf("Locked %d new prediction(s); ledger holds %d.%n",
                added.size(), ledger.size());
        long correct = scored.stream().filter(Tracker.ScoredPrediction::correct).count();
        if (!scored.isEmpty()) {
            System.out.printf("Scored %d: %d correct (%.1f%%), multiclass Brier %.4f%n",
                    scored.size(), correct, 100.0 * correct / scored.size(),
                    scored.stream().mapToDouble(Tracker.ScoredPrediction::brier)
                            .average().orElse(0));
        } else {
            System.out.println("No locked predictions resolved yet.");
        }
        System.out.println("README updated.");
    }

    private static void runSimulation(List<Match> matches, Path csv) throws IOException {
        EloRatingSystem elo = new EloRatingSystem();
        matches.forEach(elo::processMatch);

        List<Match> playedGroup = matches.stream()
                .filter(Match::isWorldCupFinals)
                .filter(m -> m.date().getYear() == 2026)
                .toList();
        List<Fixture> remaining = new MatchCsvParser().parseFixtures(csv).stream()
                .filter(Fixture::isWorldCupFinals)
                .toList();

        int runs = 10_000;
        TournamentSimulator simulator = new TournamentSimulator(elo);
        List<TournamentSimulator.TeamOdds> odds =
                simulator.simulate(playedGroup, remaining, runs, 2026L);

        System.out.printf("=== Monte Carlo: %,d simulations of the remaining tournament ===%n", runs);
        System.out.printf("Group results so far: %d played, %d fixtures remaining.%n%n",
                playedGroup.size(), remaining.size());
        System.out.printf("%4s %-22s %7s %7s %7s%n", "", "Team", "Title", "Final", "Semis");
        int rank = 1;
        for (TournamentSimulator.TeamOdds o : odds.subList(0, Math.min(15, odds.size()))) {
            System.out.printf("%3d. %-22s %6.1f%% %6.1f%% %6.1f%%%n",
                    rank++, o.team(),
                    100 * o.titleShare(), 100 * o.finalShare(), 100 * o.semiShare());
        }
        System.out.println();
        System.out.println("Simplifications: Elo tie-breaks instead of goal difference; seeded");
        System.out.println("knockout pairings; knockout draws folded into the win probability.");
    }

    private static void runUpcoming(List<Match> matches, Path csv) throws IOException {
        EloRatingSystem elo = new EloRatingSystem();
        matches.forEach(elo::processMatch);

        List<Fixture> upcoming = new MatchCsvParser().parseFixtures(csv).stream()
                .filter(Fixture::isWorldCupFinals)
                .sorted(Comparator.comparing(Fixture::date))
                .toList();

        System.out.printf("=== Upcoming World Cup fixtures: model view (%d matches) ===%n%n",
                upcoming.size());
        System.out.printf("%-10s %-44s %5s %6s %5s%n", "Date", "Match (with current Elo)",
                "Win", "Draw", "Loss");
        for (Fixture f : upcoming) {
            DrawModel.Probabilities p =
                    elo.outcomeProbabilities(f.homeTeam(), f.awayTeam(), f.neutralVenue());
            String label = String.format("%s (%.0f) vs %s (%.0f)%s",
                    f.homeTeam(), elo.ratingOf(f.homeTeam()),
                    f.awayTeam(), elo.ratingOf(f.awayTeam()),
                    f.neutralVenue() ? "" : " [home]");
            System.out.printf("%-10s %-44s %4.0f%% %5.0f%% %4.0f%%%n",
                    f.date(), label,
                    100 * p.homeWin(), 100 * p.draw(), 100 * p.awayWin());
        }
    }

    /** Usage: {@code --predict=TeamA,TeamB} (add {@code ,home} if TeamA hosts). */
    private static void runPredict(List<Match> matches, List<String> arguments) {
        String spec = arguments.stream()
                .filter(a -> a.startsWith("--predict="))
                .findFirst().orElseThrow()
                .substring("--predict=".length());
        String[] parts = spec.split(",");
        if (parts.length < 2) {
            System.out.println("Usage: --predict=TeamA,TeamB[,home]");
            return;
        }
        String home = parts[0].trim();
        String away = parts[1].trim();
        boolean neutral = parts.length < 3 || !parts[2].trim().equalsIgnoreCase("home");

        EloRatingSystem elo = new EloRatingSystem();
        matches.forEach(elo::processMatch);

        DrawModel.Probabilities p = elo.outcomeProbabilities(home, away, neutral);
        System.out.printf("%s (Elo %.0f) vs %s (Elo %.0f)%s%n",
                home, elo.ratingOf(home), away, elo.ratingOf(away),
                neutral ? " — neutral venue" : " — " + home + " at home");
        System.out.printf("  %s win: %.1f%%%n", home, 100 * p.homeWin());
        System.out.printf("  Draw:   %.1f%%%n", 100 * p.draw());
        System.out.printf("  %s win: %.1f%%%n", away, 100 * p.awayWin());
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
