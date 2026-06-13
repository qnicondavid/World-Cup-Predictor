package com.david.worldcup.goals;

import com.david.worldcup.elo.Backtest;
import com.david.worldcup.elo.DrawModel;
import com.david.worldcup.model.Match;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

/**
 * Evaluates a {@link GoalModel} the same way {@link Backtest} evaluates Elo: for
 * each World Cup it fits only on matches played in the {@code trainingYears}
 * before the tournament, then predicts every finals match three-way and scores
 * it with the multiclass Brier score. The model never sees a result before it
 * has predicted it.
 *
 * <p>Goal models are fit in batch (not online), so unlike the Elo replay there
 * is no learning <em>during</em> the tournament — every match in a given World
 * Cup is predicted from the same pre-tournament fit. That is the harder, more
 * honest regime for a tournament forecaster.
 */
public final class GoalModelBacktest {

    /** Fits a model on a training block ending at {@code asof}. */
    public interface Factory {
        GoalModel fit(List<Match> training, LocalDate asof);
    }

    private final int trainingYears;

    public GoalModelBacktest(int trainingYears) {
        this.trainingYears = trainingYears;
    }

    public Backtest.ThreeWayResult run(List<Match> all, Backtest.Window window, Factory factory) {
        LocalDate start = window.from();
        LocalDate trainStart = start.minusYears(trainingYears);
        List<Match> training = all.stream()
                .filter(m -> m.date().isBefore(start) && !m.date().isBefore(trainStart))
                .sorted(Comparator.comparing(Match::date))
                .toList();
        List<Match> test = all.stream()
                .filter(Match::isWorldCupFinals)
                .filter(m -> !m.date().isBefore(start) && !m.date().isAfter(window.until()))
                .toList();
        return score(factory.fit(training, start), test);
    }

    public Backtest.ThreeWayResult runCombined(List<Match> all, List<Backtest.Window> windows,
                                               Factory factory) {
        int evaluated = 0;
        int correct = 0;
        int drawsPredicted = 0;
        int actualDraws = 0;
        double brierSum = 0.0;
        for (Backtest.Window w : windows) {
            Backtest.ThreeWayResult r = run(all, w, factory);
            evaluated += r.matchesEvaluated();
            correct += r.correct();
            drawsPredicted += r.drawsPredicted();
            actualDraws += r.actualDraws();
            brierSum += r.multiclassBrier() * r.matchesEvaluated();
        }
        return new Backtest.ThreeWayResult(evaluated, correct,
                evaluated == 0 ? 0.0 : (double) correct / evaluated,
                evaluated == 0 ? 0.0 : brierSum / evaluated,
                drawsPredicted, actualDraws);
    }

    private static Backtest.ThreeWayResult score(GoalModel model, List<Match> test) {
        int evaluated = 0;
        int correct = 0;
        int drawsPredicted = 0;
        int actualDraws = 0;
        double brierSum = 0.0;
        for (Match m : test) {
            DrawModel.Probabilities p =
                    model.probabilities(m.homeTeam(), m.awayTeam(), m.neutralVenue());
            double[] probs = {p.homeWin(), p.draw(), p.awayWin()};
            int actual = switch (m.outcome()) {
                case HOME_WIN -> 0;
                case DRAW -> 1;
                case AWAY_WIN -> 2;
            };
            for (int i = 0; i < 3; i++) {
                double target = i == actual ? 1.0 : 0.0;
                brierSum += (probs[i] - target) * (probs[i] - target);
            }
            int predicted = 0;
            if (probs[1] > probs[predicted]) {
                predicted = 1;
            }
            if (probs[2] > probs[predicted]) {
                predicted = 2;
            }
            if (predicted == actual) {
                correct++;
            }
            if (predicted == 1) {
                drawsPredicted++;
            }
            if (actual == 1) {
                actualDraws++;
            }
            evaluated++;
        }
        return new Backtest.ThreeWayResult(evaluated, correct,
                evaluated == 0 ? 0.0 : (double) correct / evaluated,
                evaluated == 0 ? 0.0 : brierSum / evaluated,
                drawsPredicted, actualDraws);
    }
}
