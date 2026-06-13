package com.david.worldcup.rest;

import com.david.worldcup.model.Match;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RestBacktestTest {

    private static final LocalDate FROM = LocalDate.of(2018, 6, 1);
    private static final LocalDate UNTIL = LocalDate.of(2018, 7, 31);

    /** A and B are equal; A enters the World Cup match with 8 days rest, B with 2, and A wins. */
    private static List<Match> scenario() {
        return List.of(
                new Match(LocalDate.of(2018, 6, 7), "A", "C", 1, 1, "Friendly", true),
                new Match(LocalDate.of(2018, 6, 13), "B", "D", 1, 1, "Friendly", true),
                new Match(LocalDate.of(2018, 6, 15), "A", "B", 1, 0, "FIFA World Cup", true));
    }

    @Test
    void restAdvantageHelpsWhenTheRestedTeamWins() {
        RestBacktest bt = new RestBacktest();
        RestBacktest.Result base = bt.run(scenario(), FROM, UNTIL, 0);
        RestBacktest.Result tuned = bt.run(scenario(), FROM, UNTIL, 20);

        assertEquals(1, base.evaluated());
        assertEquals(1, tuned.evaluated());
        // crediting the better-rested winner sharpens the probability -> lower Brier
        assertTrue(tuned.multiclassBrier() < base.multiclassBrier(),
                "rest-adjusted Brier " + tuned.multiclassBrier()
                        + " should beat baseline " + base.multiclassBrier());
    }

    @Test
    void onlyWorldCupMatchesInsideTheWindowAreScored() {
        RestBacktest bt = new RestBacktest();
        RestBacktest.Result r = bt.run(scenario(), FROM, UNTIL, 0);
        assertEquals(1, r.evaluated()); // the two friendlies are training only
    }

    @Test
    void zeroCoefficientIsDeterministicBaseline() {
        RestBacktest bt = new RestBacktest();
        // identical inputs and zero rest weight must reproduce exactly
        assertEquals(bt.run(scenario(), FROM, UNTIL, 0).multiclassBrier(),
                bt.run(scenario(), FROM, UNTIL, 0).multiclassBrier(), 0.0);
    }
}
