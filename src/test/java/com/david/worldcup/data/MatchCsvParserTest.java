package com.david.worldcup.data;

import com.david.worldcup.model.Match;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MatchCsvParserTest {

    @Test
    void parsesRegularRow() {
        Optional<Match> result = MatchCsvParser.parseLine(
                "2022-12-18,Argentina,France,3,3,FIFA World Cup,Lusail,Qatar,TRUE");

        assertTrue(result.isPresent());
        Match match = result.get();
        assertEquals(LocalDate.of(2022, 12, 18), match.date());
        assertEquals("Argentina", match.homeTeam());
        assertEquals("France", match.awayTeam());
        assertEquals(3, match.homeScore());
        assertEquals(3, match.awayScore());
        assertEquals("FIFA World Cup", match.tournament());
        assertTrue(match.neutralVenue());
        assertEquals(Match.Outcome.DRAW, match.outcome());
    }

    @Test
    void parsesQuotedFieldContainingComma() {
        List<String> fields = MatchCsvParser.splitCsvLine(
                "2026-06-12,USA,Mexico,1,0,Friendly,\"Washington, D.C.\",United States,FALSE");

        assertEquals(9, fields.size());
        assertEquals("Washington, D.C.", fields.get(6));
    }

    @Test
    void skipsUnplayedFixtures() {
        Optional<Match> result = MatchCsvParser.parseLine(
                "2026-06-27,Panama,England,NA,NA,FIFA World Cup,East Rutherford,United States,TRUE");
        assertTrue(result.isEmpty());
    }

    @Test
    void skipsMalformedRows() {
        assertTrue(MatchCsvParser.parseLine("not,a,real,row").isEmpty());
        assertTrue(MatchCsvParser.parseLine("").isEmpty());
        assertTrue(MatchCsvParser.parseLine(
                "yesterday,A,B,1,0,Friendly,City,Country,FALSE").isEmpty());
    }

    @Test
    void parsesNonNeutralVenueAsFalse() {
        Optional<Match> result = MatchCsvParser.parseLine(
                "1950-07-16,Brazil,Uruguay,1,2,FIFA World Cup,Rio de Janeiro,Brazil,FALSE");
        assertTrue(result.isPresent());
        assertTrue(!result.get().neutralVenue());
        assertEquals(Match.Outcome.AWAY_WIN, result.get().outcome());
    }
}
