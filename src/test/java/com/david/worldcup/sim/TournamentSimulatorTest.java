package com.david.worldcup.sim;

import com.david.worldcup.elo.EloRatingSystem;
import com.david.worldcup.model.Fixture;
import com.david.worldcup.model.Match;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TournamentSimulatorTest {

    private static final LocalDate D = LocalDate.of(2026, 6, 20);

    private static Fixture fx(String home, String away) {
        return new Fixture(D, home, away, "FIFA World Cup", true);
    }

    @Test
    void inferGroupsFindsConnectedComponents() {
        List<Fixture> fixtures = List.of(
                fx("A1", "A2"), fx("A3", "A4"), fx("A1", "A3"),
                fx("B1", "B2"), fx("B3", "B4"), fx("B2", "B4"));

        List<Set<String>> groups = TournamentSimulator.inferGroups(List.of(), fixtures);

        assertEquals(2, groups.size());
        assertTrue(groups.contains(Set.of("A1", "A2", "A3", "A4")));
        assertTrue(groups.contains(Set.of("B1", "B2", "B3", "B4")));
    }

    @Test
    void muchStrongerTeamWinsMostSimulatedTitles() {
        // Two groups of four; make "Star" far stronger than everyone else.
        EloRatingSystem elo = new EloRatingSystem();
        for (int i = 0; i < 30; i++) {
            elo.processMatch(new Match(D.minusDays(60).plusDays(i),
                    "Star", "Sparring", 4, 0, "FIFA World Cup", true));
        }

        List<Fixture> fixtures = new ArrayList<>();
        String[][] groups = {
                {"Star", "A2", "A3", "A4"},
                {"B1", "B2", "B3", "B4"}};
        for (String[] g : groups) {
            for (int i = 0; i < g.length; i++) {
                for (int j = i + 1; j < g.length; j++) {
                    fixtures.add(fx(g[i], g[j]));
                }
            }
        }

        List<TournamentSimulator.TeamOdds> odds =
                new TournamentSimulator(elo).simulate(List.of(), fixtures, 2000, 42L);

        assertEquals("Star", odds.get(0).team());
        assertTrue(odds.get(0).titleShare() > 0.6,
                "expected dominant title share, got " + odds.get(0).titleShare());
        double total = odds.stream().mapToDouble(TournamentSimulator.TeamOdds::titleShare).sum();
        assertEquals(1.0, total, 1e-9); // every simulation crowns exactly one champion
    }

    @Test
    void playedResultsAreRespected() {
        // A4 already beat everyone: with no remaining fixtures it must win the group.
        EloRatingSystem elo = new EloRatingSystem();
        List<Match> played = List.of(
                new Match(D, "A4", "A1", 1, 0, "FIFA World Cup", true),
                new Match(D, "A4", "A2", 1, 0, "FIFA World Cup", true),
                new Match(D, "A4", "A3", 1, 0, "FIFA World Cup", true),
                new Match(D, "B1", "B2", 1, 0, "FIFA World Cup", true),
                new Match(D, "B1", "B3", 1, 0, "FIFA World Cup", true),
                new Match(D, "B1", "B4", 1, 0, "FIFA World Cup", true),
                new Match(D, "A1", "A2", 1, 1, "FIFA World Cup", true),
                new Match(D, "B2", "B3", 1, 1, "FIFA World Cup", true));

        List<TournamentSimulator.TeamOdds> odds =
                new TournamentSimulator(elo).simulate(played, List.of(), 500, 7L);

        // Group winners A4 and B1 must always reach the (4-team) knockout;
        // between equal-rated teams the title splits roughly evenly among qualifiers.
        double a4 = odds.stream().filter(o -> o.team().equals("A4"))
                .findFirst().orElseThrow().titleShare();
        assertTrue(a4 > 0.1, "group winner should contend, got " + a4);
    }
}
