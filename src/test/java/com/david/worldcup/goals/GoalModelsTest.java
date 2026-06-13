package com.david.worldcup.goals;

import com.david.worldcup.elo.DrawModel;
import com.david.worldcup.model.Match;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Deterministic checks: a synthetic league where A > B > C consistently. We do
 * not assert exact numbers (the real-data comparison runs via {@code --goals});
 * we assert the qualitative properties any correct fit must have.
 */
class GoalModelsTest {

    private static final LocalDate ASOF = LocalDate.of(2024, 1, 1);

    private static List<Match> league() {
        List<Match> matches = new ArrayList<>();
        LocalDate d = LocalDate.of(2021, 1, 1);
        for (int r = 0; r < 25; r++) {
            d = d.plusDays(10);
            matches.add(new Match(d, "A", "C", 3, 0, "Friendly", false));
            matches.add(new Match(d.plusDays(1), "B", "C", 2, 0, "Friendly", false));
            matches.add(new Match(d.plusDays(2), "A", "B", 2, 1, "Friendly", false));
            matches.add(new Match(d.plusDays(3), "C", "A", 0, 2, "Friendly", true));
            matches.add(new Match(d.plusDays(4), "C", "B", 1, 2, "Friendly", true));
        }
        return matches;
    }

    @Test
    void fitterRecoversStrengthOrdering() {
        TeamStrength s = new PoissonRatingsFitter().halfLifeYears(10).fit(league(), ASOF);
        assertTrue(s.attackOf("A") > s.attackOf("B"), "A should out-attack B");
        assertTrue(s.attackOf("B") > s.attackOf("C"), "B should out-attack C");
        // higher defence value = concedes more, so the strongest side has the lowest
        assertTrue(s.defenceOf("A") < s.defenceOf("C"), "A should concede less than C");
    }

    @Test
    void dixonColesFavoursTheStrongerTeamAndNormalises() {
        DixonColesModel m = DixonColesModel.fit(league(), ASOF);
        DrawModel.Probabilities p = m.probabilities("A", "C", true);
        assertEquals(1.0, p.homeWin() + p.draw() + p.awayWin(), 1e-9);
        assertTrue(p.homeWin() > 0.6, "A should be a clear favourite over C, was " + p.homeWin());
        assertTrue(p.homeWin() > p.awayWin());
    }

    @Test
    void bivariateModelProducesValidFavouredProbabilities() {
        BivariatePoissonModel m = BivariatePoissonModel.fit(league(), ASOF);
        DrawModel.Probabilities p = m.probabilities("A", "C", true);
        assertEquals(1.0, p.homeWin() + p.draw() + p.awayWin(), 1e-9);
        assertTrue(p.homeWin() > 0.6);
        assertTrue(m.lambda3() >= 0.0);
    }

    @Test
    void eloPoissonFavoursTheStrongerTeamAndNormalises() {
        EloPoissonModel m = EloPoissonModel.fit(league());
        DrawModel.Probabilities p = m.probabilities("A", "C", true);
        assertEquals(1.0, p.homeWin() + p.draw() + p.awayWin(), 1e-9);
        assertTrue(p.homeWin() > p.awayWin(), "A should be favoured over C");
    }

    @Test
    void unknownTeamsAreTreatedAsAverage() {
        DixonColesModel m = DixonColesModel.fit(league(), ASOF);
        DrawModel.Probabilities p = m.probabilities("Atlantis", "Wakanda", true);
        assertEquals(1.0, p.homeWin() + p.draw() + p.awayWin(), 1e-9);
        // two unknown (average) teams on neutral ground: symmetric
        assertEquals(p.homeWin(), p.awayWin(), 1e-9);
    }
}
