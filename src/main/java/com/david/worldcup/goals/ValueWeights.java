package com.david.worldcup.goals;

/**
 * How strongly squad market value pulls a team's fitted attack/defence ratings
 * toward the value-implied prior.
 *
 * <p>The shrinkage weight for a team with {@code n} training matches is
 * {@code clamp(globalWeight + sparseWeight * kappa/(n + kappa), 0, 1)}:
 * <ul>
 *   <li>{@code globalWeight} applies to every team (the "blend" lever);</li>
 *   <li>{@code sparseWeight} adds extra pull for teams with little data — at
 *       {@code n = 0} it contributes its full value, fading as matches accumulate
 *       (the "prior for sparse teams" lever);</li>
 *   <li>{@code kappa} is the match count at which the sparse term is halved.</li>
 * </ul>
 * {@code valueScale} maps a one-standard-deviation richer squad to this many
 * log-goals of attack (and the same reduction in goals conceded).
 *
 * <p>The defaults were tuned by {@code --values-tune}: a grid search on World
 * Cups 2006-2018, validated once on held-out 2022 (where they beat plain
 * Dixon-Coles, Brier 0.6065 vs 0.6123). The notable outcome is
 * {@code sparseWeight = 0}: at World Cup level no team is data-starved, so the
 * sparse-team prior does nothing and it is the uniform {@code globalWeight}
 * blend that helps. The winner sat at the edge of the search grid, so a wider
 * sweep may find a slightly stronger setting.
 */
public record ValueWeights(double globalWeight, double sparseWeight, double kappa, double valueScale) {

    public static final ValueWeights DEFAULT = new ValueWeights(0.20, 0.00, 5.0, 0.30);

    /** Shrinkage weight toward the value prior for a team seen {@code matches} times. */
    public double shrinkageFor(int matches) {
        double w = globalWeight + sparseWeight * kappa / (matches + kappa);
        return Math.max(0.0, Math.min(1.0, w));
    }
}
