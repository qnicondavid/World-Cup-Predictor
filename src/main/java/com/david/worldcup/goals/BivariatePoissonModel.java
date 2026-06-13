package com.david.worldcup.goals;

import com.david.worldcup.elo.DrawModel;
import com.david.worldcup.model.Match;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Bivariate Poisson goal model. It reuses the Dixon-Coles attack/defence fit for
 * the marginal goal rates, then adds a single shared component {@code lambda3}
 * that makes the two teams' scores positively correlated. {@code lambda3} is
 * chosen by a one-dimensional search over the (time-weighted) training
 * likelihood.
 */
public final class BivariatePoissonModel implements GoalModel {

    private final TeamStrength strength;
    private final double lambda3;

    public BivariatePoissonModel(TeamStrength strength, double lambda3) {
        this.strength = strength;
        this.lambda3 = lambda3;
    }

    public static BivariatePoissonModel fit(List<Match> training, LocalDate asof) {
        TeamStrength s = new PoissonRatingsFitter().fit(training, asof);
        double xi = Math.log(2.0) / (2.0 * 365.25); // matches the fitter's default half-life

        double best = 0.0;
        double bestLl = Double.NEGATIVE_INFINITY;
        for (int step = 0; step <= 30; step++) {
            double l3 = 0.02 * step; // 0.00 .. 0.60
            double ll = 0.0;
            for (Match mt : training) {
                double lh = s.lambdaHome(mt.homeTeam(), mt.awayTeam(), mt.neutralVenue());
                double la = s.lambdaAway(mt.homeTeam(), mt.awayTeam(), mt.neutralVenue());
                double cap = Math.min(l3, 0.9 * Math.min(lh, la));
                double p = bivariatePmf(mt.homeScore(), mt.awayScore(), lh, la, cap);
                long age = Math.max(0, ChronoUnit.DAYS.between(mt.date(), asof));
                ll += Math.exp(-xi * age) * Math.log(Math.max(p, 1e-300));
            }
            if (ll > bestLl) {
                bestLl = ll;
                best = l3;
            }
        }
        return new BivariatePoissonModel(s, best);
    }

    public double lambda3() {
        return lambda3;
    }

    /** Joint probability of the scoreline (x, y) under a bivariate Poisson. */
    static double bivariatePmf(int x, int y, double lambdaHome, double lambdaAway, double lambda3) {
        double l3 = Math.min(lambda3, 0.9 * Math.min(lambdaHome, lambdaAway));
        l3 = Math.max(l3, 0.0);
        double l1 = Math.max(lambdaHome - l3, 1e-9);
        double l2 = Math.max(lambdaAway - l3, 1e-9);
        double base = Math.exp(-(l1 + l2 + l3));
        double sum = 0.0;
        int kMax = Math.min(x, y);
        for (int k = 0; k <= kMax; k++) {
            sum += Math.pow(l1, x - k) / factorial(x - k)
                    * Math.pow(l2, y - k) / factorial(y - k)
                    * Math.pow(l3, k) / factorial(k);
        }
        return base * sum;
    }

    private static double factorial(int k) {
        double f = 1.0;
        for (int i = 2; i <= k; i++) {
            f *= i;
        }
        return f;
    }

    @Override
    public DrawModel.Probabilities probabilities(String home, String away, boolean neutral) {
        double lh = strength.lambdaHome(home, away, neutral);
        double la = strength.lambdaAway(home, away, neutral);
        return ScoreGrid.bivariate(lh, la, lambda3);
    }

    @Override
    public String name() {
        return "Bivariate Poisson";
    }
}
