package com.david.worldcup.goals;

import com.david.worldcup.elo.DrawModel;
import com.david.worldcup.elo.EloConfig;
import com.david.worldcup.elo.EloRatingSystem;
import com.david.worldcup.model.Match;

import java.util.Comparator;
import java.util.List;

/**
 * The lightweight option: reuse the existing Elo engine for team strength, then
 * map the Elo rating gap to two Poisson goal rates.
 *
 * <p>Let {@code s} be the effective Elo gap (in hundreds of points, recovered by
 * inverting the Elo win-probability logistic). Goal rates are
 * {@code lambdaHome = exp(c0 + c1*s)} and {@code lambdaAway = exp(c0 - c2*s)};
 * the three coefficients are fit by Poisson regression (diagonal Newton steps)
 * on the training matches. Win/draw/loss then follow from two independent
 * Poisson distributions.
 */
public final class EloPoissonModel implements GoalModel {

    private final EloRatingSystem elo;
    private final double c0;
    private final double c1;
    private final double c2;

    private EloPoissonModel(EloRatingSystem elo, double c0, double c1, double c2) {
        this.elo = elo;
        this.c0 = c0;
        this.c1 = c1;
        this.c2 = c2;
    }

    public static EloPoissonModel fit(List<Match> training) {
        List<Match> ordered = training.stream()
                .sorted(Comparator.comparing(Match::date))
                .toList();
        int m = ordered.size();
        double[] s = new double[m];
        double[] hg = new double[m];
        double[] ag = new double[m];

        EloRatingSystem elo = new EloRatingSystem(EloConfig.DEFAULT);
        for (int k = 0; k < m; k++) {
            Match mt = ordered.get(k);
            double e = clamp(elo.winProbability(mt.homeTeam(), mt.awayTeam(), mt.neutralVenue()));
            s[k] = gap(e) / 100.0;
            hg[k] = mt.homeScore();
            ag[k] = mt.awayScore();
            elo.processMatch(mt); // predict before learning
        }

        double c0 = Math.log(1.3);
        double c1 = 0.3;
        double c2 = 0.3;
        for (int iter = 0; iter < 100; iter++) {
            double g0 = 0, g1 = 0, g2 = 0, h0 = 0, h1 = 0, h2 = 0;
            for (int k = 0; k < m; k++) {
                double lh = Math.exp(c0 + c1 * s[k]);
                double la = Math.exp(c0 - c2 * s[k]);
                g0 += (hg[k] - lh) + (ag[k] - la);
                h0 += lh + la;
                g1 += s[k] * (hg[k] - lh);
                h1 += s[k] * s[k] * lh;
                g2 += -s[k] * (ag[k] - la);
                h2 += s[k] * s[k] * la;
            }
            if (h0 > 0) {
                c0 += g0 / h0;
            }
            if (h1 > 0) {
                c1 += g1 / h1;
            }
            if (h2 > 0) {
                c2 += g2 / h2;
            }
        }
        return new EloPoissonModel(elo, c0, c1, c2);
    }

    private static double clamp(double e) {
        return Math.max(1e-6, Math.min(1 - 1e-6, e));
    }

    private static double gap(double e) {
        return 400.0 * Math.log10(e / (1 - e));
    }

    @Override
    public DrawModel.Probabilities probabilities(String home, String away, boolean neutral) {
        double e = clamp(elo.winProbability(home, away, neutral));
        double sv = gap(e) / 100.0;
        double lh = Math.exp(c0 + c1 * sv);
        double la = Math.exp(c0 - c2 * sv);
        return ScoreGrid.independent(lh, la);
    }

    @Override
    public String name() {
        return "Elo-Poisson";
    }
}
