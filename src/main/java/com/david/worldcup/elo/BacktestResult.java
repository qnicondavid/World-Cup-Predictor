package com.david.worldcup.elo;

/**
 * Outcome of one backtest run.
 *
 * @param matchesEvaluated   number of matches the model was scored on
 * @param correctPredictions matches where the model's favorite won (draws count as misses)
 * @param accuracy           correctPredictions / matchesEvaluated
 * @param brierScore         mean squared error of the predicted probabilities;
 *                           lower is better, 0.25 = no better than a coin flip
 */
public record BacktestResult(
        int matchesEvaluated,
        int correctPredictions,
        double accuracy,
        double brierScore) {

    public String summary() {
        return String.format("%d/%d correct (%.1f%%), Brier score %.4f",
                correctPredictions, matchesEvaluated, accuracy * 100, brierScore);
    }
}
