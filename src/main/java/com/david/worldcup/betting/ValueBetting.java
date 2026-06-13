package com.david.worldcup.betting;

import java.util.Optional;

/**
 * The value engine: compare model probabilities to bookmaker odds, recommend a
 * bet when there is enough expected-value edge, and size it with fractional
 * Kelly. Also settles a bet against an actual result for ROI tracking.
 *
 * <p>For a decimal price {@code o} and model probability {@code p}, expected
 * value per unit staked is {@code p*o - 1}. The full Kelly stake is
 * {@code (p*o - 1) / (o - 1)}; we take a fraction of it and cap it.
 */
public final class ValueBetting {

    private ValueBetting() {
    }

    /**
     * The best +EV bet on this match, or empty if no outcome clears
     * {@link BettingConfig#minEdge()}.
     */
    public static Optional<ValueBet> evaluate(double[] modelProbs, Odds odds, BettingConfig config) {
        int best = -1;
        double bestEv = config.minEdge();
        for (int i = 0; i < 3; i++) {
            double ev = modelProbs[i] * odds.oddsFor(i) - 1.0;
            if (ev > bestEv) {
                bestEv = ev;
                best = i;
            }
        }
        if (best < 0) {
            return Optional.empty();
        }
        double o = odds.oddsFor(best);
        double p = modelProbs[best];
        double fullKelly = (p * o - 1.0) / (o - 1.0);
        double stake = Math.max(0.0, Math.min(config.maxStakeFraction(), config.kellyFraction() * fullKelly));
        return Optional.of(new ValueBet(odds.date(), odds.homeTeam(), odds.awayTeam(), best,
                p, odds.fairProbabilities()[best], o, bestEv, stake));
    }

    /** Profit (or loss) per unit bankroll once the bet is settled against {@code actualOutcome}. */
    public static double settle(ValueBet bet, int actualOutcome) {
        return bet.outcome() == actualOutcome
                ? bet.stakeFraction() * (bet.offeredOdds() - 1.0)
                : -bet.stakeFraction();
    }
}
