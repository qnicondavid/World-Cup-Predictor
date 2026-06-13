package com.david.worldcup.betting;

import com.david.worldcup.data.MatchCsvParser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Bookmaker odds per fixture, read from a CSV with columns
 * {@code match_date,home_team,away_team,home_odds,draw_odds,away_odds} (decimal
 * odds). One row per fixture. If the file is absent the table is empty and every
 * lookup misses, so callers degrade to "no odds, no bets".
 */
public final class OddsTable {

    private final Map<String, Odds> byMatch;

    private OddsTable(Map<String, Odds> byMatch) {
        this.byMatch = byMatch;
    }

    public static OddsTable load(Path file) throws IOException {
        Map<String, Odds> byMatch = new HashMap<>();
        if (!Files.exists(file)) {
            return new OddsTable(byMatch);
        }
        List<String> lines = Files.readAllLines(file);
        for (String line : lines.subList(Math.min(1, lines.size()), lines.size())) {
            if (line.isBlank()) {
                continue;
            }
            List<String> f = MatchCsvParser.splitCsvLine(line);
            if (f.size() < 6) {
                continue;
            }
            try {
                LocalDate date = LocalDate.parse(f.get(0).trim());
                String home = f.get(1).trim();
                String away = f.get(2).trim();
                Odds odds = new Odds(date, home, away,
                        Double.parseDouble(f.get(3).trim()),
                        Double.parseDouble(f.get(4).trim()),
                        Double.parseDouble(f.get(5).trim()));
                byMatch.put(key(date, home, away), odds);
            } catch (NumberFormatException | DateTimeParseException ignored) {
                // skip malformed rows
            }
        }
        return new OddsTable(byMatch);
    }

    public boolean isEmpty() {
        return byMatch.isEmpty();
    }

    public Optional<Odds> oddsFor(LocalDate date, String home, String away) {
        return Optional.ofNullable(byMatch.get(key(date, home, away)));
    }

    private static String key(LocalDate date, String home, String away) {
        return date + "|" + home + "|" + away;
    }
}
