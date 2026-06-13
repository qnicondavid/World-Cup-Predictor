package com.david.worldcup.elo;

/**
 * Splits the Elo expected score into explicit win / draw / loss probabilities.
 *
 * <p>The Elo expected score E conflates winning with drawing: E = P(win) + P(draw)/2.
 * To unpick it we need P(draw), which we estimate empirically: across 37,314
 * internationals since 1980, the draw rate falls from ~30% between equal teams
 * to ~2% at a 600-point effective rating gap. The table below holds the observed
 * draw rate per 50-point gap bin (computed by replaying the dataset through this
 * very Elo model); lookups interpolate linearly between bins.
 *
 * <p>Given P(draw), the split is: P(win) = E − P(draw)/2, P(loss) = 1 − P(win) − P(draw),
 * with clamping so nothing goes negative for extreme gaps.
 */
public final class DrawModel {

    /** Observed draw rates at gap = 0, 50, 100, ... 600 (internationals since 1980). */
    private static final double[] DRAW_RATE_BY_GAP = {
            0.299, 0.289, 0.270, 0.252, 0.243, 0.200, 0.181,
            0.150, 0.125, 0.102, 0.076, 0.041, 0.022
    };
    private static final double BIN_WIDTH = 50.0;

    public record Probabilities(double homeWin, double draw, double awayWin) {

        public Probabilities {
            double sum = homeWin + draw + awayWin;
            if (Math.abs(sum - 1.0) > 1e-9) {
                throw new IllegalArgumentException("probabilities sum to " + sum);
            }
        }
    }

    /** Empirical P(draw) for a given effective rating gap (sign is ignored). */
    public static double drawProbability(double ratingGap) {
        double gap = Math.abs(ratingGap);
        double maxGap = (DRAW_RATE_BY_GAP.length - 1) * BIN_WIDTH;
        if (gap >= maxGap) {
            return DRAW_RATE_BY_GAP[DRAW_RATE_BY_GAP.length - 1];
        }
        int bin = (int) (gap / BIN_WIDTH);
        double t = (gap - bin * BIN_WIDTH) / BIN_WIDTH;
        return DRAW_RATE_BY_GAP[bin] * (1 - t) + DRAW_RATE_BY_GAP[bin + 1] * t;
    }

    /**
     * Win/draw/loss probabilities for the home side, given the Elo expected
     * score and the effective rating gap it was computed from.
     */
    public static Probabilities split(double expectedScore, double ratingGap) {
        double pDraw = drawProbability(ratingGap);
        double pWin = expectedScore - pDraw / 2.0;
        pWin = Math.max(0.0, Math.min(1.0 - pDraw, pWin));
        return new Probabilities(pWin, pDraw, 1.0 - pDraw - pWin);
    }

    private DrawModel() {
    }
}
