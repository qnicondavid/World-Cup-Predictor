package com.david.worldcup.elo;

import com.david.worldcup.model.Match;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BacktestTest {

    private static final double EPSILON = 1e-9;
    private static final LocalDate WINDOW_START = LocalDate.of(2022, 11, 1);
    private static final LocalDate WINDOW_END = LocalDate.of(2022, 12, 31);

    private static Match worldCupMatch(LocalDate date, String home, String away,
                                       int homeGoals, int awayGoals) {
        return new Match(date, home, away, homeGoals, awayGoals, "FIFA World Cup", true);
    }

    @Test
    void evenMatchupIsScoredAsCoinFlip() {
        // No training history: both teams at 1500, neutral venue -> p = 0.5 exactly.
        List<Match> matches = List.of(
                worldCupMatch(LocalDate.of(2022, 11, 21), "A", "B", 1, 0));

        BacktestResult result = new Backtest().run(matches, WINDOW_START, WINDOW_END);

        assertEquals(1, result.matchesEvaluated());
        assertEquals(1, result.correctPredictions()); // p >= 0.5 favors home, home won
        assertEquals(0.25, result.brierScore(), EPSILON); // (0.5 - 1.0)^2
    }

    @Test
    void drawCountsAsMissWithZeroBrierAtEvenOdds() {
        List<Match> matches = List.of(
                worldCupMatch(LocalDate.of(2022, 11, 21), "A", "B", 1, 1));

        BacktestResult result = new Backtest().run(matches, WINDOW_START, WINDOW_END);

        assertEquals(1, result.matchesEvaluated());
        assertEquals(0, result.correctPredictions());
        assertEquals(0.0, result.accuracy(), EPSILON);
        assertEquals(0.0, result.brierScore(), EPSILON); // (0.5 - 0.5)^2
    }

    @Test
    void nonFinalsMatchesInsideWindowAreNotScored() {
        List<Match> matches = List.of(
                new Match(LocalDate.of(2022, 11, 21), "A", "B", 1, 0, "Friendly", true));

        BacktestResult result = new Backtest().run(matches, WINDOW_START, WINDOW_END);
        assertEquals(0, result.matchesEvaluated());
    }

    @Test
    void matchesBeforeWindowAreTrainingOnly() {
        List<Match> matches = List.of(
                worldCupMatch(LocalDate.of(2018, 7, 15), "A", "B", 2, 0),
                worldCupMatch(LocalDate.of(2022, 11, 21), "A", "B", 1, 0));

        BacktestResult result = new Backtest().run(matches, WINDOW_START, WINDOW_END);

        assertEquals(1, result.matchesEvaluated());
        // The 2018 result trained the model, so A is now rated above B and the
        // prediction is more confident than a coin flip: brier < 0.25.
        assertTrue(result.brierScore() < 0.25);
    }

    @Test
    void matchesAfterWindowAreIgnored() {
        List<Match> matches = List.of(
                worldCupMatch(LocalDate.of(2026, 6, 13), "A", "B", 1, 0));

        BacktestResult result = new Backtest().run(matches, WINDOW_START, WINDOW_END);
        assertEquals(0, result.matchesEvaluated());
    }

    @Test
    void modelKeepsLearningInsideTheWindow() {
        // Same result twice: the second prediction should be sharper because the
        // model learned from the first. Mean brier therefore drops below 0.25.
        List<Match> matches = List.of(
                worldCupMatch(LocalDate.of(2022, 11, 21), "A", "B", 1, 0),
                worldCupMatch(LocalDate.of(2022, 11, 25), "A", "B", 1, 0));

        BacktestResult result = new Backtest().run(matches, WINDOW_START, WINDOW_END);

        assertEquals(2, result.matchesEvaluated());
        assertEquals(2, result.correctPredictions());
        assertTrue(result.brierScore() < 0.25,
                "expected learning to sharpen the second prediction, got "
                        + result.brierScore());
    }
}
