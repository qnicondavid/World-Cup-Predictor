package com.david.worldcup.tracker;

import com.david.worldcup.data.MatchCsvParser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Persistent record of predictions, stored as CSV and committed to git.
 *
 * <p>This file is the project's honesty mechanism: a prediction is appended
 * ("locked") before the match is played and never modified afterwards, so the
 * git history proves every prediction predates its result.
 */
public final class PredictionLedger {

    private static final String HEADER =
            "match_date,home_team,away_team,neutral,p_home,locked_on";

    /**
     * @param matchDate    date of the predicted match
     * @param homeTeam     first-listed team
     * @param awayTeam     second-listed team
     * @param neutralVenue venue flag used for the prediction
     * @param pHome        locked probability (Elo expected score) for the home side
     * @param lockedOn     date the prediction was locked
     */
    public record Prediction(
            LocalDate matchDate,
            String homeTeam,
            String awayTeam,
            boolean neutralVenue,
            double pHome,
            LocalDate lockedOn) {

        public String favorite() {
            return pHome >= 0.5 ? homeTeam : awayTeam;
        }

        public double favoriteProbability() {
            return pHome >= 0.5 ? pHome : 1 - pHome;
        }
    }

    public static List<Prediction> load(Path file) throws IOException {
        List<Prediction> predictions = new ArrayList<>();
        if (!Files.exists(file)) {
            return predictions;
        }
        List<String> lines = Files.readAllLines(file);
        for (String line : lines.subList(Math.min(1, lines.size()), lines.size())) {
            if (line.isBlank()) {
                continue;
            }
            List<String> f = MatchCsvParser.splitCsvLine(line);
            predictions.add(new Prediction(
                    LocalDate.parse(f.get(0)),
                    f.get(1),
                    f.get(2),
                    Boolean.parseBoolean(f.get(3)),
                    Double.parseDouble(f.get(4)),
                    LocalDate.parse(f.get(5))));
        }
        return predictions;
    }

    public static void save(Path file, List<Prediction> predictions) throws IOException {
        if (file.getParent() != null) {
            Files.createDirectories(file.getParent());
        }
        StringBuilder sb = new StringBuilder(HEADER).append('\n');
        for (Prediction p : predictions) {
            sb.append(String.join(",",
                    p.matchDate().toString(),
                    p.homeTeam(),
                    p.awayTeam(),
                    String.valueOf(p.neutralVenue()),
                    String.format(Locale.ROOT, "%.4f", p.pHome()),
                    p.lockedOn().toString())).append('\n');
        }
        Files.writeString(file, sb.toString());
    }

    private PredictionLedger() {
    }
}
