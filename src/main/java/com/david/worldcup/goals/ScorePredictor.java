package com.david.worldcup.goals;

/**
 * Turns an Elo rating gap into a predicted scoreline, keeping the score
 * consistent with the win/draw/loss pick (both come from the same gap).
 *
 * <p>Expected goals are a logistic-free mapping of the gap:
 * <pre>
 *   s          = effectiveGap / 100              (rating gap in "hundreds")
 *   homeGoals  = BASE_GOALS * exp( SENSITIVITY * s)
 *   awayGoals  = BASE_GOALS * exp(-SENSITIVITY * s)
 * </pre>
 * so an even match expects {@value #BASE_GOALS} goals a side and a mismatch
 * tilts the goals toward the stronger team (and raises the total — blowouts are
 * higher-scoring). The most likely scoreline is the mode of each side's Poisson.
 *
 * <p>The constants are a documented default calibrated to typical international
 * scoring; they can be replaced with a proper fit (see {@link EloPoissonModel},
 * which estimates the same shape by regression). Because the prediction is a
 * deterministic function of the (locked) Elo gap, a predicted score can be
 * recovered for any past prediction without storing extra data.
 */
public final class ScorePredictor {

    public static final double BASE_GOALS = 1.30;
    public static final double SENSITIVITY = 0.25;

    public record PredictedScore(double homeGoals, double awayGoals, int modalHome, int modalAway) {

        /** Total absolute goal difference between the predicted scoreline and the actual one. */
        public int goalError(int actualHome, int actualAway) {
            return Math.abs(modalHome - actualHome) + Math.abs(modalAway - actualAway);
        }

        /** Whether the predicted scoreline exactly matched the actual result. */
        public boolean exact(int actualHome, int actualAway) {
            return modalHome == actualHome && modalAway == actualAway;
        }
    }

    private ScorePredictor() {
    }

    /** Builds a predicted scoreline directly from a model's expected goals. */
    public static PredictedScore fromExpectedGoals(double homeGoals, double awayGoals) {
        return new PredictedScore(homeGoals, awayGoals,
                ScoreGrid.mode(homeGoals), ScoreGrid.mode(awayGoals));
    }

    /** Predicts a scoreline from an Elo expected score (E = P(win) + P(draw)/2). */
    public static PredictedScore fromExpectedScore(double expectedScore) {
        double e = Math.max(1e-6, Math.min(1 - 1e-6, expectedScore));
        return fromGap(400.0 * Math.log10(e / (1 - e)));
    }

    /** Predicts a scoreline from an effective Elo rating gap (home minus away). */
    public static PredictedScore fromGap(double effectiveGap) {
        double s = effectiveGap / 100.0;
        double homeGoals = BASE_GOALS * Math.exp(SENSITIVITY * s);
        double awayGoals = BASE_GOALS * Math.exp(-SENSITIVITY * s);
        return new PredictedScore(homeGoals, awayGoals,
                ScoreGrid.mode(homeGoals), ScoreGrid.mode(awayGoals));
    }
}
