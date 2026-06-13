package com.david.worldcup.goals;

import com.david.worldcup.elo.DrawModel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScoreGridTest {

    private static final double TOL = 1e-5;

    @Test
    void independentProbabilitiesMatchReferenceAndNormalise() {
        DrawModel.Probabilities p = ScoreGrid.independent(1.5, 1.0);
        assertEquals(0.487945, p.homeWin(), TOL);
        assertEquals(0.259848, p.draw(), TOL);
        assertEquals(0.252207, p.awayWin(), TOL);
        assertEquals(1.0, p.homeWin() + p.draw() + p.awayWin(), 1e-9);
    }

    @Test
    void equalRatesGiveSymmetricProbabilities() {
        DrawModel.Probabilities p = ScoreGrid.independent(1.3, 1.3);
        assertEquals(p.homeWin(), p.awayWin(), 1e-12);
        assertEquals(0.263914, p.draw(), TOL);
    }

    @Test
    void dixonColesWithZeroRhoEqualsIndependent() {
        DrawModel.Probabilities dc = ScoreGrid.dixonColes(1.5, 1.0, 0.0);
        DrawModel.Probabilities ind = ScoreGrid.independent(1.5, 1.0);
        assertEquals(ind.homeWin(), dc.homeWin(), 1e-12);
        assertEquals(ind.draw(), dc.draw(), 1e-12);
        assertEquals(ind.awayWin(), dc.awayWin(), 1e-12);
    }

    @Test
    void negativeRhoLiftsDrawProbability() {
        DrawModel.Probabilities p = ScoreGrid.dixonColes(1.5, 1.0, -0.1);
        assertEquals(0.475633, p.homeWin(), TOL);
        assertEquals(0.284473, p.draw(), TOL);
        assertEquals(0.239894, p.awayWin(), TOL);
        // the low-score correction with rho<0 raises draws above the plain Poisson value
        assertTrue(p.draw() > ScoreGrid.independent(1.5, 1.0).draw());
    }

    @Test
    void bivariateWithZeroCovarianceEqualsIndependent() {
        DrawModel.Probabilities biv = ScoreGrid.bivariate(1.5, 1.0, 0.0);
        DrawModel.Probabilities ind = ScoreGrid.independent(1.5, 1.0);
        assertEquals(ind.homeWin(), biv.homeWin(), 1e-9);
        assertEquals(ind.draw(), biv.draw(), 1e-9);
        assertEquals(ind.awayWin(), biv.awayWin(), 1e-9);
    }
}
