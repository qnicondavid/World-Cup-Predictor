package com.david.worldcup.elo;

/**
 * Tunable parameters of the Elo model.
 *
 * @param kWorldCup         K-factor for World Cup finals matches
 * @param kQualification    K-factor for qualifiers
 * @param kOther            K-factor for other competitive matches
 * @param kFriendly         K-factor for friendlies
 * @param homeAdvantage     rating bonus for the home team at non-neutral venues
 * @param goalMarginScaling if true, K is multiplied by a margin factor
 *                          (1.5 for a two-goal win, 1.75 for three, ...)
 * @param annualRegression  fraction by which every rating is pulled back toward
 *                          1500 at each calendar-year boundary (0 = off). Models
 *                          squad turnover: old results should fade.
 */
public record EloConfig(
        double kWorldCup,
        double kQualification,
        double kOther,
        double kFriendly,
        double homeAdvantage,
        boolean goalMarginScaling,
        double annualRegression) {

    /** Phase 1 parameters: eloratings.net-style constants, no margin scaling. */
    public static final EloConfig BASELINE =
            new EloConfig(60, 40, 35, 20, 100, false, 0.0);

    /** Best parameters from grid search: tuned on WC 2006-2018, validated on WC 2022. */
    public static final EloConfig DEFAULT =
            new EloConfig(60, 40, 35, 30, 50, true, 0.0);

    double kFactor(String tournament) {
        String t = tournament.toLowerCase();
        if (t.equals("fifa world cup")) return kWorldCup;
        if (t.contains("qualification")) return kQualification;
        if (t.contains("friendly")) return kFriendly;
        return kOther;
    }
}
