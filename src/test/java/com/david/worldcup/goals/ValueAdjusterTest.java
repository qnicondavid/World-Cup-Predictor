package com.david.worldcup.goals;

import com.david.worldcup.value.MarketValueTable;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ValueAdjusterTest {

    private static MarketValueTable threeTeams() throws Exception {
        Path tmp = Files.createTempFile("values", ".csv");
        Files.writeString(tmp,
                "team,as_of,value_eur\n"
                        + "Rich,2020-01-01,1000000000\n"
                        + "Mid,2020-01-01,100000000\n"
                        + "Poor,2020-01-01,10000000\n");
        MarketValueTable t = MarketValueTable.load(tmp);
        Files.deleteIfExists(tmp);
        return t;
    }

    @Test
    void asOfLookupNeverReturnsAFutureValuation() throws Exception {
        MarketValueTable t = threeTeams();
        assertTrue(t.valueAsOf("Rich", LocalDate.of(2020, 6, 1)).isPresent());
        assertFalse(t.valueAsOf("Rich", LocalDate.of(2019, 1, 1)).isPresent()); // before any snapshot
        assertFalse(t.valueAsOf("Unknown", LocalDate.of(2020, 6, 1)).isPresent());
    }

    @Test
    void richSparseTeamIsBoostedTowardTheValuePrior() throws Exception {
        // all three teams start average (attack 0, defence 0) and have little data
        TeamStrength flat = new TeamStrength(Math.log(1.3), 0.2, -0.05,
                Map.of("Rich", 0.0, "Mid", 0.0, "Poor", 0.0),
                Map.of("Rich", 0.0, "Mid", 0.0, "Poor", 0.0));
        Map<String, Integer> counts = Map.of("Rich", 1, "Mid", 1, "Poor", 1);

        TeamStrength adjusted = ValueAdjuster.adjust(flat, counts, threeTeams(),
                LocalDate.of(2020, 6, 1), ValueWeights.DEFAULT);

        assertTrue(adjusted.attackOf("Rich") > adjusted.attackOf("Poor"),
                "the richer squad should get a higher attack prior");
        assertTrue(adjusted.defenceOf("Rich") < adjusted.defenceOf("Poor"),
                "the richer squad should be expected to concede less");
    }

    @Test
    void emptyTableLeavesStrengthUntouched() throws Exception {
        TeamStrength flat = new TeamStrength(0.0, 0.0, 0.0,
                Map.of("A", 0.3), Map.of("A", -0.1));
        Path tmp = Files.createTempFile("empty", ".csv");
        Files.writeString(tmp, "team,as_of,value_eur\n");
        MarketValueTable empty = MarketValueTable.load(tmp);
        Files.deleteIfExists(tmp);

        TeamStrength same = ValueAdjuster.adjust(flat, Map.of("A", 5), empty,
                LocalDate.of(2020, 6, 1), ValueWeights.DEFAULT);
        assertEquals(0.3, same.attackOf("A"), 1e-9);
    }
}
