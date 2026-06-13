package com.david.worldcup.betting;

import java.time.LocalDate;

/**
 * A recommended bet: the model thinks outcome {@code outcome} is more likely
 * than the bookmaker's price implies.
 *
 * @param outcome          0=home, 1=draw, 2=away
 * @param modelProbability the model's probability for that outcome
 * @param fairProbability  the de-vigged bookmaker probability for that outcome
 * @param offeredOdds      the decimal odds being offered
 * @param expectedValue    expected profit per unit staked ({@code modelProbability*offeredOdds - 1})
 * @param stakeFraction    recommended stake as a fraction of bankroll
 */
public record ValueBet(LocalDate date, String homeTeam, String awayTeam, int outcome,
                       double modelProbability, double fairProbability, double offeredOdds,
                       double expectedValue, double stakeFraction) {

    public String outcomeLabel() {
        return switch (outcome) {
            case 0 -> homeTeam;
            case 1 -> "Draw";
            default -> awayTeam;
        };
    }
}
