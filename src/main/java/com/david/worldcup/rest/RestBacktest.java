package com.david.worldcup.rest;

import com.david.worldcup.elo.DrawModel;
import com.david.worldcup.elo.EloConfig;
import com.david.worldcup.elo.EloRatingSystem;
import com.david.worldcup.model.Match;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Tests one hypothesis: a team that comes into a match with more recovery than
 * its opponent has an edge the plain rating ignores.
 *
 * <p>The feature is a rest-days <em>differential</em>: replaying matches in date
 * order, we track each team's previous match date and, before a match is scored,
 * compute {@code homeRest - awayRest} (each capped at {@value #REST_CAP_DAYS}
 * days, so a long pre-tournament break does not read as a huge advantage). That
 * differential is converted to rating points ({@code pointsPerDay}) and added to
 * the home side's effective rating, then win/draw/loss follow from the usual Elo
 * expected score and {@link DrawModel}.
 *
 * <p>With {@code pointsPerDay == 0} this is exactly the Elo + DrawModel baseline,
 * so the same harness measures whether rest actually lowers the held-out Brier.
 * It is evaluated in the same predict-before-you-learn regime as {@code Backtest}.
 */
public final class RestBacktest {

    /** Rest beyond this many days is treated as "fully rested" (no extra credit). */
    public static final int REST_CAP_DAYS = 10;

    private static final double HOME_ADVANTAGE = EloConfig.DEFAULT.homeAdvantage();

    public record Result(int evaluated, int correct, double accuracy, double multiclassBrier) {
        public String summary() {
            return String.format(Locale.ROOT, "%d/%d correct (%.1f%%), multiclass Brier %.4f",
                    correct, evaluated, accuracy * 100, multiclassBrier);
        }
    }

    /**
     * Replays {@code matches}, scoring World Cup finals matches in
     * [{@code from}, {@code until}] with the rest-adjusted model.
     *
     * @param pointsPerDay rating points added per day of rest advantage (0 = baseline)
     */
    public Result run(List<Match> matches, LocalDate from, LocalDate until, double pointsPerDay) {
        List<Match> ordered = new ArrayList<>(matches);
        ordered.sort(Comparator.comparing(Match::date));

        EloRatingSystem elo = new EloRatingSystem();
        Map<String, LocalDate> lastPlayed = new HashMap<>();

        int evaluated = 0;
        int correct = 0;
        double brierSum = 0.0;

        for (Match m : ordered) {
            if (m.date().isAfter(until)) {
                break;
            }
            if (m.isWorldCupFinals() && !m.date().isBefore(from)) {
                double restDiff = restDays(lastPlayed, m.homeTeam(), m.date())
                        - restDays(lastPlayed, m.awayTeam(), m.date());
                double effHome = elo.ratingOf(m.homeTeam())
                        + (m.neutralVenue() ? 0.0 : HOME_ADVANTAGE)
                        + pointsPerDay * restDiff;
                double awayRating = elo.ratingOf(m.awayTeam());
                double e = EloRatingSystem.expectedScore(effHome, awayRating);
                DrawModel.Probabilities p = DrawModel.split(e, effHome - awayRating);
                double[] probs = {p.homeWin(), p.draw(), p.awayWin()};

                int actual = switch (m.outcome()) {
                    case HOME_WIN -> 0;
                    case DRAW -> 1;
                    case AWAY_WIN -> 2;
                };
                for (int i = 0; i < 3; i++) {
                    double target = i == actual ? 1.0 : 0.0;
                    brierSum += (probs[i] - target) * (probs[i] - target);
                }
                int predicted = 0;
                if (probs[1] > probs[predicted]) {
                    predicted = 1;
                }
                if (probs[2] > probs[predicted]) {
                    predicted = 2;
                }
                if (predicted == actual) {
                    correct++;
                }
                evaluated++;
            }

            // Learn only after predicting (rest and rating both update post-match).
            lastPlayed.put(m.homeTeam(), m.date());
            lastPlayed.put(m.awayTeam(), m.date());
            elo.processMatch(m);
        }

        return new Result(evaluated, correct,
                evaluated == 0 ? 0.0 : (double) correct / evaluated,
                evaluated == 0 ? 0.0 : brierSum / evaluated);
    }

    /** Days since a team last played, capped; a team with no prior match counts as fully rested. */
    private static double restDays(Map<String, LocalDate> lastPlayed, String team, LocalDate date) {
        LocalDate prev = lastPlayed.get(team);
        if (prev == null) {
            return REST_CAP_DAYS;
        }
        long days = ChronoUnit.DAYS.between(prev, date);
        return Math.min(Math.max(days, 0), REST_CAP_DAYS);
    }
}
