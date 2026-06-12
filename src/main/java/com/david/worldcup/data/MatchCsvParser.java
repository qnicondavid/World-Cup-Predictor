package com.david.worldcup.data;

import com.david.worldcup.model.Match;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Parses the international results CSV from
 * https://github.com/martj42/international_results
 *
 * <p>Expected columns:
 * {@code date,home_team,away_team,home_score,away_score,tournament,city,country,neutral}
 *
 * <p>Rows with non-numeric scores (e.g. {@code NA} for fixtures not yet played)
 * are silently skipped — they are fixtures, not results.
 */
public final class MatchCsvParser {

    private static final int EXPECTED_COLUMNS = 9;

    public List<Match> parse(Path csvFile) throws IOException {
        List<Match> matches = new ArrayList<>();
        try (BufferedReader reader = Files.newBufferedReader(csvFile)) {
            String header = reader.readLine(); // skip header row
            if (header == null) {
                return matches;
            }
            String line;
            while ((line = reader.readLine()) != null) {
                parseLine(line).ifPresent(matches::add);
            }
        }
        return matches;
    }

    /**
     * Parses one CSV row into a {@link Match}, or {@link Optional#empty()} if the
     * row is malformed or represents an unplayed fixture.
     */
    static Optional<Match> parseLine(String line) {
        List<String> fields = splitCsvLine(line);
        if (fields.size() < EXPECTED_COLUMNS) {
            return Optional.empty();
        }
        try {
            return Optional.of(new Match(
                    LocalDate.parse(fields.get(0)),
                    fields.get(1),
                    fields.get(2),
                    Integer.parseInt(fields.get(3)),
                    Integer.parseInt(fields.get(4)),
                    fields.get(5),
                    Boolean.parseBoolean(fields.get(8))));
        } catch (NumberFormatException | DateTimeParseException e) {
            // "NA" scores = future fixture; bad date = malformed row. Skip both.
            return Optional.empty();
        }
    }

    /**
     * Minimal quote-aware CSV field splitter. Handles fields like
     * {@code "Washington, D.C."} that contain commas inside double quotes.
     */
    static List<String> splitCsvLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                fields.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        fields.add(current.toString());
        return fields;
    }
}
