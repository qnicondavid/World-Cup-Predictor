package com.david.worldcup.betting;

/**
 * Staking policy for value betting.
 *
 * @param minEdge          minimum expected value per unit stake to act (e.g. 0.05 = require a 5% edge)
 * @param kellyFraction    fraction of the full Kelly stake to use (e.g. 0.25 = quarter-Kelly)
 * @param maxStakeFraction hard cap on the stake as a fraction of bankroll
 */
public record BettingConfig(double minEdge, double kellyFraction, double maxStakeFraction) {

    /**
     * Deliberately conservative: a 5% edge floor, quarter-Kelly, capped at 5% of
     * bankroll. The calibration audit found the model's confidence drifts year to
     * year, so thin edges are not trustworthy and stakes stay small.
     */
    public static final BettingConfig DEFAULT = new BettingConfig(0.05, 0.25, 0.05);
}
