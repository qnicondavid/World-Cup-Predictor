package com.david.worldcup.betting;

import java.time.LocalDate;

/**
 * Decimal bookmaker odds for one match's three outcomes.
 *
 * <p>Raw implied probabilities ({@code 1/odds}) sum to more than 1 — the excess
 * is the bookmaker's margin ("overround" or vig). {@link #fairProbabilities}
 * removes it by proportional normalisation, the standard simple de-vig; it is
 * what the book "really" thinks before its cut. (More elaborate methods exist —
 * Shin, power — but proportional is the transparent default.)
 */
public record Odds(LocalDate date, String homeTeam, String awayTeam,
                   double homeOdds, double drawOdds, double awayOdds) {

    /** Decimal odds for outcome 0=home, 1=draw, 2=away. */
    public double oddsFor(int outcome) {
        return switch (outcome) {
            case 0 -> homeOdds;
            case 1 -> drawOdds;
            default -> awayOdds;
        };
    }

    /** Raw implied probabilities (sum &gt; 1 by the overround). */
    public double[] impliedProbabilities() {
        return new double[] {1.0 / homeOdds, 1.0 / drawOdds, 1.0 / awayOdds};
    }

    /** The bookmaker's margin: how much the implied probabilities exceed 1. */
    public double overround() {
        double[] r = impliedProbabilities();
        return r[0] + r[1] + r[2] - 1.0;
    }

    /** Margin-free probabilities, proportional-normalised to sum to 1. */
    public double[] fairProbabilities() {
        double[] r = impliedProbabilities();
        double sum = r[0] + r[1] + r[2];
        return new double[] {r[0] / sum, r[1] / sum, r[2] / sum};
    }
}
