package com.david.worldcup.elo;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DrawModelTest {

    private static final double EPSILON = 1e-9;

    @Test
    void probabilitiesAlwaysSumToOne() {
        for (double gap = -800; gap <= 800; gap += 37) {
            double expected = EloRatingSystem.expectedScore(1500 + gap, 1500);
            DrawModel.Probabilities p = DrawModel.split(expected, gap);
            assertEquals(1.0, p.homeWin() + p.draw() + p.awayWin(), EPSILON);
            assertTrue(p.homeWin() >= 0 && p.draw() >= 0 && p.awayWin() >= 0);
        }
    }

    @Test
    void drawProbabilityDecreasesAsGapGrows() {
        double previous = Double.MAX_VALUE;
        for (double gap = 0; gap <= 600; gap += 50) {
            double current = DrawModel.drawProbability(gap);
            assertTrue(current <= previous, "draw prob should not rise with gap");
            previous = current;
        }
    }

    @Test
    void equalTeamsAreSymmetricAroundTheDraw() {
        DrawModel.Probabilities p = DrawModel.split(0.5, 0);
        assertEquals(p.homeWin(), p.awayWin(), EPSILON);
        assertTrue(p.draw() > 0.25 && p.draw() < 0.35, "got " + p.draw());
    }

    @Test
    void gapSignDoesNotMatter() {
        assertEquals(DrawModel.drawProbability(250), DrawModel.drawProbability(-250), EPSILON);
    }

    @Test
    void hugeFavoriteAlmostNeverDrawsOrLoses() {
        double expected = EloRatingSystem.expectedScore(2200, 1400);
        DrawModel.Probabilities p = DrawModel.split(expected, 800);
        assertTrue(p.homeWin() > 0.9);
        assertTrue(p.draw() < 0.05);
    }
}
