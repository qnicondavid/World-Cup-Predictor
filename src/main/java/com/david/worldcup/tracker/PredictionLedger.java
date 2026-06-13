package com.david.worldcup.tracker;

import com.david.worldcup.data.MatchCsvParser;
import com.david.worldcup.elo.DrawModel;
import com.david.worldcup.elo.EloRatingSystem;
import com.david.worldcup.model.Match;

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
 *
 * <p>Each prediction stores an explicit win/draw/loss split so it can be scored
 * three-way. Predictions locked under the older binary schema (a single
 * {@code p_home} Elo expected score) are read transparently and expanded into a
 * split by the deterministic {@link DrawModel}; the locked expected score is
 * preserved exactly (it equals {@code pHome + pDraw / 2}).
 */
public final class PredictionLedger {

    private static final String HEADER =
            "match_date,home_team,away_team,neutral,p_home,p_draw,p_away,locked_on";

    /** Columns in the legacy schema: ...,neutral,p_home,locked_on (no draw/away). */
    private static final int LEGACY_COLUMNS = 6;

    /**
     * @param matchDate    date of the predicted match
     * @param homeTeam     first-listed team
     * @param awayTeam     second-listed team
     * @param neutralVenue venue flag used for the prediction
     * @param pHome        locked probability of a home win
     * @param pDraw        locked probability of a draw
     * @param pAway        locked probability of an away win
     * @param lockedOn     date the prediction was locked
     */
    public record Prediction(
            LocalDate matchDate,
            String homeTeam,
            String awayTeam,
            boolean neutralVenue,
            double pHome,
            double pDraw,
            double pAway,
            LocalDate lockedOn) {

        /**
         * Legacy binary constructor: expands a single locked win probability (the
         * Elo expected score {@code E = P(win) + P(draw)/2}) into an explicit
         * win/draw/loss split via the deterministic {@link DrawModel}. The locked
         * expected score is not changed — it is recoverable as {@code pHome + pDraw/2}.
         */
        public Prediction(LocalDate matchDate, String homeTeam, String awayTeam,
                          boolean neutralVenue, double expectedScore, LocalDate lockedOn) {
            this(matchDate, homeTeam, awayTeam, neutralVenue,
                    expand(expectedScore), lockedOn);
        }

        private Prediction(LocalDate matchDate, String homeTeam, String awayTeam,
                           boolean neutralVenue, DrawModel.Probabilities split, LocalDate lockedOn) {
            this(matchDate, homeTeam, awayTeam, neutralVenue,
                    split.homeWin(), split.draw(), split.awayWin(), lockedOn);
        }

        /** The model's single most likely outcome. */
        public Match.Outcome predictedOutcome() {
            if (pDraw >= pHome && pDraw >= pAway) {
                return Match.Outcome.DRAW;
            }
            return pHome >= pAway ? Match.Outcome.HOME_WIN : Match.Outcome.AWAY_WIN;
        }

        /** Human-readable pick: a team name, or "Draw". */
        public String pick() {
            return switch (predictedOutcome()) {
                case HOME_WIN -> homeTeam;
                case AWAY_WIN -> awayTeam;
                case DRAW -> "Draw";
            };
        }

        /** Probability assigned to {@link #pick()} (the largest of the three). */
        public double pickProbability() {
            return Math.max(pHome, Math.max(pDraw, pAway));
        }

        /**
         * Recovers the win/draw/loss split implied by an Elo expected score, by
         * inverting the logistic to get the effective rating gap and feeding it
         * through {@link DrawModel}. This reproduces exactly what
         * {@link EloRatingSystem#outcomeProbabilities} produces at lock time.
         */
        private static DrawModel.Probabilities expand(double expectedScore) {
            double e = Math.max(1e-9, Math.min(1.0 - 1e-9, expectedScore));
            double effectiveGap = 400.0 * Math.log10(e / (1.0 - e));
            return DrawModel.split(e, effectiveGap);
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
            predictions.add(parse(MatchCsvParser.splitCsvLine(line)));
        }
        return predictions;
    }

    private static Prediction parse(List<String> f) {
        if (f.size() <= LEGACY_COLUMNS) {
            // Legacy schema: match_date,home,away,neutral,p_home(expected score),locked_on
            return new Prediction(
                    LocalDate.parse(f.get(0)),
                    f.get(1),
                    f.get(2),
                    Boolean.parseBoolean(f.get(3)),
                    Double.parseDouble(f.get(4)),
                    LocalDate.parse(f.get(5)));
        }
        return new Prediction(
                LocalDate.parse(f.get(0)),
                f.get(1),
                f.get(2),
                Boolean.parseBoolean(f.get(3)),
                Double.parseDouble(f.get(4)),
                Double.parseDouble(f.get(5)),
                Double.parseDouble(f.get(6)),
                LocalDate.parse(f.get(7)));
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
                    String.format(Locale.ROOT, "%.4f", p.pDraw()),
                    String.format(Locale.ROOT, "%.4f", p.pAway()),
                    p.lockedOn().toString())).append('\n');
        }
        Files.writeString(file, sb.toString());
    }

    private PredictionLedger() {
    }
}
