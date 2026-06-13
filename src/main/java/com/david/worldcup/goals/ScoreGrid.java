package com.david.worldcup.goals;

import com.david.worldcup.elo.DrawModel;

/**
 * Turns expected goal rates into win / draw / loss probabilities by summing a
 * scoreline grid (home goals 0..{@value #MAX_GOALS} by away goals 0..{@value #MAX_GOALS}).
 *
 * <p>Three joint distributions are supported:
 * <ul>
 *   <li>{@link #independent} — two independent Poisson distributions.</li>
 *   <li>{@link #dixonColes} — independent Poisson with the Dixon-Coles low-score
 *       correction (parameter {@code rho}) that fixes the well-known
 *       under-prediction of 0-0 and 1-1 draws.</li>
 *   <li>{@link #bivariate} — a bivariate Poisson whose shared component
 *       {@code lambda3} induces positive correlation between the two scores.</li>
 * </ul>
 */
public final class ScoreGrid {

    /** Scorelines are truncated at this many goals per team (probability beyond is negligible). */
    public static final int MAX_GOALS = 10;

    private ScoreGrid() {
    }

    /** Poisson probability mass for 0..MAX_GOALS, built by the recurrence p(k)=p(k-1)*lambda/k. */
    static double[] poissonPmf(double lambda) {
        double[] p = new double[MAX_GOALS + 1];
        double term = Math.exp(-lambda);
        p[0] = term;
        for (int k = 1; k <= MAX_GOALS; k++) {
            term *= lambda / k;
            p[k] = term;
        }
        return p;
    }

    /** Most likely goal count for a Poisson rate (the distribution's mode). */
    public static int mode(double lambda) {
        double[] p = poissonPmf(lambda);
        int best = 0;
        for (int k = 1; k <= MAX_GOALS; k++) {
            if (p[k] > p[best]) {
                best = k;
            }
        }
        return best;
    }

    /** Collapses a scoreline grid into normalised home-win / draw / away-win probabilities. */
    static DrawModel.Probabilities wdl(double[][] grid) {
        double home = 0, draw = 0, away = 0;
        for (int x = 0; x <= MAX_GOALS; x++) {
            for (int y = 0; y <= MAX_GOALS; y++) {
                double v = grid[x][y];
                if (x > y) {
                    home += v;
                } else if (x == y) {
                    draw += v;
                } else {
                    away += v;
                }
            }
        }
        double sum = home + draw + away;
        return new DrawModel.Probabilities(home / sum, draw / sum, away / sum);
    }

    public static DrawModel.Probabilities independent(double lambdaHome, double lambdaAway) {
        double[] ph = poissonPmf(lambdaHome);
        double[] pa = poissonPmf(lambdaAway);
        double[][] g = new double[MAX_GOALS + 1][MAX_GOALS + 1];
        for (int x = 0; x <= MAX_GOALS; x++) {
            for (int y = 0; y <= MAX_GOALS; y++) {
                g[x][y] = ph[x] * pa[y];
            }
        }
        return wdl(g);
    }

    public static DrawModel.Probabilities dixonColes(double lambdaHome, double lambdaAway, double rho) {
        double[] ph = poissonPmf(lambdaHome);
        double[] pa = poissonPmf(lambdaAway);
        double[][] g = new double[MAX_GOALS + 1][MAX_GOALS + 1];
        for (int x = 0; x <= MAX_GOALS; x++) {
            for (int y = 0; y <= MAX_GOALS; y++) {
                g[x][y] = ph[x] * pa[y];
            }
        }
        g[0][0] *= 1 - lambdaHome * lambdaAway * rho;
        g[0][1] *= 1 + lambdaHome * rho;
        g[1][0] *= 1 + lambdaAway * rho;
        g[1][1] *= 1 - rho;
        for (int x = 0; x < 2; x++) {
            for (int y = 0; y < 2; y++) {
                if (g[x][y] < 0) {
                    g[x][y] = 0;
                }
            }
        }
        return wdl(g);
    }

    public static DrawModel.Probabilities bivariate(double lambdaHome, double lambdaAway, double lambda3) {
        double l3 = Math.min(lambda3, 0.9 * Math.min(lambdaHome, lambdaAway));
        l3 = Math.max(l3, 0.0);
        double l1 = Math.max(lambdaHome - l3, 1e-9);
        double l2 = Math.max(lambdaAway - l3, 1e-9);
        double[] fact = new double[MAX_GOALS + 1];
        fact[0] = 1;
        for (int k = 1; k <= MAX_GOALS; k++) {
            fact[k] = fact[k - 1] * k;
        }
        double base = Math.exp(-(l1 + l2 + l3));
        double[][] g = new double[MAX_GOALS + 1][MAX_GOALS + 1];
        for (int x = 0; x <= MAX_GOALS; x++) {
            for (int y = 0; y <= MAX_GOALS; y++) {
                double s = 0.0;
                int kMax = Math.min(x, y);
                for (int k = 0; k <= kMax; k++) {
                    s += Math.pow(l1, x - k) / fact[x - k]
                            * Math.pow(l2, y - k) / fact[y - k]
                            * Math.pow(l3, k) / fact[k];
                }
                g[x][y] = base * s;
            }
        }
        return wdl(g);
    }
}
