package com.david.worldcup.goals;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScorePredictorTest {

    @Test
    void evenMatchPredictsADrawAtTheBaseRate() {
        ScorePredictor.PredictedScore ps = ScorePredictor.fromExpectedScore(0.5);
        assertEquals(ScorePredictor.BASE_GOALS, ps.homeGoals(), 1e-9);
        assertEquals(ScorePredictor.BASE_GOALS, ps.awayGoals(), 1e-9);
        assertEquals(1, ps.modalHome());
        assertEquals(1, ps.modalAway());
    }

    @Test
    void strongFavouriteTiltsGoalsAndScoreline() {
        // Canada's locked expected score vs Bosnia (E = 0.8429)
        ScorePredictor.PredictedScore ps = ScorePredictor.fromExpectedScore(0.8429);
        assertEquals(2.70, ps.homeGoals(), 0.02);
        assertEquals(0.63, ps.awayGoals(), 0.02);
        assertEquals(2, ps.modalHome());
        assertEquals(0, ps.modalAway());
    }

    @Test
    void goalErrorAndExactFlag() {
        ScorePredictor.PredictedScore ps = ScorePredictor.fromExpectedScore(0.8429); // 2-0
        assertEquals(2, ps.goalError(1, 1));      // |2-1| + |0-1|
        assertFalse(ps.exact(1, 1));
        assertEquals(0, ps.goalError(2, 0));
        assertTrue(ps.exact(2, 0));
    }

    @Test
    void symmetryUnderpinsAwayFavourites() {
        ScorePredictor.PredictedScore home = ScorePredictor.fromExpectedScore(0.8429);
        ScorePredictor.PredictedScore away = ScorePredictor.fromExpectedScore(1 - 0.8429);
        // mirror image: the away side's goals equal the home side's in the flipped fixture
        assertEquals(home.homeGoals(), away.awayGoals(), 1e-9);
        assertEquals(home.awayGoals(), away.homeGoals(), 1e-9);
    }
}
