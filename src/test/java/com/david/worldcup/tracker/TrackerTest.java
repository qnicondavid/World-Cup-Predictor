package com.david.worldcup.tracker;

import com.david.worldcup.elo.EloRatingSystem;
import com.david.worldcup.model.Fixture;
import com.david.worldcup.model.Match;
import com.david.worldcup.tracker.PredictionLedger.Prediction;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TrackerTest {

    private static final double EPSILON = 1e-9;
    private static final LocalDate TODAY = LocalDate.of(2026, 6, 12);

    @Test
    void locksOnlyUpcomingWorldCupFixturesNotAlreadyInLedger() {
        List<Fixture> fixtures = List.of(
                new Fixture(TODAY.plusDays(1), "A", "B", "FIFA World Cup", true),
                new Fixture(TODAY.plusDays(2), "C", "D", "FIFA World Cup", true),
                new Fixture(TODAY.minusDays(1), "E", "F", "FIFA World Cup", true), // past
                new Fixture(TODAY.plusDays(1), "G", "H", "Friendly", true));      // not WC

        List<Prediction> ledger = List.of(
                new Prediction(TODAY.plusDays(1), "A", "B", true, 0.5, TODAY)); // already locked

        List<Prediction> added = Tracker.lockNewPredictions(
                new EloRatingSystem(), fixtures, ledger, TODAY);

        assertEquals(1, added.size());
        assertEquals("C", added.get(0).homeTeam());
    }

    @Test
    void scoresCorrectAndIncorrectPredictions() {
        List<Prediction> ledger = List.of(
                new Prediction(TODAY, "A", "B", true, 0.8, TODAY.minusDays(1)),
                new Prediction(TODAY, "C", "D", true, 0.3, TODAY.minusDays(1)),
                new Prediction(TODAY.plusDays(5), "E", "F", true, 0.6, TODAY)); // unplayed

        List<Match> results = List.of(
                new Match(TODAY, "A", "B", 2, 0, "FIFA World Cup", true),  // favorite won
                new Match(TODAY, "C", "D", 1, 0, "FIFA World Cup", true)); // favorite (D) lost

        List<Tracker.ScoredPrediction> scored = Tracker.score(ledger, results);

        assertEquals(2, scored.size()); // unplayed match not scored
        assertTrue(scored.get(0).correct());
        assertTrue(!scored.get(1).correct());
        assertEquals(0.04, scored.get(0).brier(), EPSILON); // (0.8 - 1.0)^2
        assertEquals(0.49, scored.get(1).brier(), EPSILON); // (0.3 - 1.0)^2
    }

    @Test
    void replaceSectionSwapsContentBetweenMarkers() {
        String readme = "# Title\n" + Tracker.SECTION_START + "\nold\n"
                + Tracker.SECTION_END + "\nfooter";
        String updated = Tracker.replaceSection(readme, "new content");
        assertTrue(updated.contains("new content"));
        assertTrue(!updated.contains("old"));
        assertTrue(updated.contains("footer"));
    }

    @Test
    void replaceSectionAppendsWhenMarkersMissing() {
        String updated = Tracker.replaceSection("# Title", "new content");
        assertTrue(updated.contains(Tracker.SECTION_START));
        assertTrue(updated.contains("new content"));
    }

    @Test
    void ledgerRoundTripsThroughCsv() throws Exception {
        Path tmp = Files.createTempFile("ledger", ".csv");
        List<Prediction> original = List.of(
                new Prediction(TODAY, "Brazil", "Morocco", true, 0.6038, TODAY));
        PredictionLedger.save(tmp, original);
        List<Prediction> loaded = PredictionLedger.load(tmp);
        assertEquals(original, loaded);
        Files.deleteIfExists(tmp);
    }
}
