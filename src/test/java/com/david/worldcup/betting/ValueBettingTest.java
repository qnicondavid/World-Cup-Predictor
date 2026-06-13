package com.david.worldcup.betting;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ValueBettingTest {

    private static final BettingConfig CONFIG = BettingConfig.DEFAULT; // 5% edge, 1/4 Kelly, 5% cap

    private static Odds odds(double h, double d, double a) {
        return new Odds(LocalDate.of(2026, 6, 13), "A", "B", h, d, a);
    }

    @Test
    void deVigRemovesOverroundAndNormalises() {
        Odds o = odds(2.0, 3.5, 4.0);
        assertEquals(0.0357, o.overround(), 1e-3);
        double[] fair = o.fairProbabilities();
        assertEquals(1.0, fair[0] + fair[1] + fair[2], 1e-9);
        assertTrue(fair[0] < o.impliedProbabilities()[0], "fair prob is below raw implied");
        assertEquals(0.4828, fair[0], 1e-3);
    }

    @Test
    void recommendsTheBestPositiveEdgeBetAndSizesIt() {
        Optional<ValueBet> bet = ValueBetting.evaluate(
                new double[] {0.6, 0.25, 0.15}, odds(2.0, 3.5, 4.0), CONFIG);
        assertTrue(bet.isPresent());
        ValueBet vb = bet.get();
        assertEquals(0, vb.outcome());                 // home
        assertEquals(0.20, vb.expectedValue(), 1e-9);  // 0.6*2 - 1
        assertEquals(0.05, vb.stakeFraction(), 1e-9);  // quarter-Kelly 0.05, at the cap
    }

    @Test
    void noBetWhenNoOutcomeClearsTheEdgeFloor() {
        Optional<ValueBet> bet = ValueBetting.evaluate(
                new double[] {0.4, 0.3, 0.3}, odds(2.0, 3.3, 3.3), CONFIG);
        assertTrue(bet.isEmpty());
    }

    @Test
    void stakeIsCappedForHugeEdges() {
        Optional<ValueBet> bet = ValueBetting.evaluate(
                new double[] {0.95, 0.03, 0.02}, odds(2.0, 12.0, 18.0), CONFIG);
        assertTrue(bet.isPresent());
        assertEquals(0.05, bet.get().stakeFraction(), 1e-9); // full Kelly 0.9 -> capped at 5%
    }

    @Test
    void settlementPaysOnWinsAndLosesTheStakeOnLosses() {
        ValueBet vb = ValueBetting.evaluate(
                new double[] {0.6, 0.25, 0.15}, odds(2.0, 3.5, 4.0), CONFIG).orElseThrow();
        assertEquals(0.05 * (2.0 - 1.0), ValueBetting.settle(vb, 0), 1e-9); // home wins
        assertEquals(-0.05, ValueBetting.settle(vb, 2), 1e-9);              // away wins
    }
}
