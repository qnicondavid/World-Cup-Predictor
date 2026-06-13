package com.david.worldcup.goals;

import java.util.Map;

/**
 * Fitted attack/defence ratings for a set of teams, on the log scale.
 *
 * <p>Expected goals for a match are
 * <pre>
 *   lambdaHome = exp(baseline + attack[home] + defence[away] + homeAdvantage*[not neutral])
 *   lambdaAway = exp(baseline + attack[away] + defence[home])
 * </pre>
 * where a higher {@code attack} means a team scores more and a higher
 * {@code defence} means it <em>concedes</em> more. Attack and defence are each
 * centred at zero, so {@code baseline} is the log goal rate of an average
 * matchup and an unknown team is treated as average (0, 0).
 *
 * @param baseline      log of the average per-team goal rate
 * @param homeAdvantage additive log-scale home bonus (applied to home lambda at non-neutral venues)
 * @param rho           Dixon-Coles low-score correction (0 = plain independent Poisson)
 * @param attack        per-team attacking strength (log scale, mean 0)
 * @param defence       per-team defensive weakness (log scale, mean 0)
 */
public record TeamStrength(
        double baseline,
        double homeAdvantage,
        double rho,
        Map<String, Double> attack,
        Map<String, Double> defence) {

    public double attackOf(String team) {
        return attack.getOrDefault(team, 0.0);
    }

    public double defenceOf(String team) {
        return defence.getOrDefault(team, 0.0);
    }

    public double lambdaHome(String home, String away, boolean neutral) {
        return Math.exp(baseline + attackOf(home) + defenceOf(away)
                + (neutral ? 0.0 : homeAdvantage));
    }

    public double lambdaAway(String home, String away, boolean neutral) {
        return Math.exp(baseline + attackOf(away) + defenceOf(home));
    }
}
