package com.david.worldcup.elo;

import com.david.worldcup.model.Match;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Elo rating system for national teams, in the style of the
 * <a href="https://www.eloratings.net/about">World Football Elo Ratings</a>.
 *
 * <p>Core ideas:
 * <ul>
 *   <li>Every team starts at {@value #INITIAL_RATING}.</li>
 *   <li>The expected score of a match is a logistic function of the rating gap.</li>
 *   <li>After each match, ratings move by {@code K * (actual - expected)} —
 *       zero-sum between the two teams.</li>
 *   <li>K depends on how much the match matters (World Cup &gt; qualifier &gt; friendly)
 *       and, optionally, on the goal margin — a 4-0 win says more than 1-0.</li>
 *   <li>Home teams get a temporary rating boost unless the venue is neutral.</li>
 * </ul>
 *
 * <p>All constants are configurable via {@link EloConfig} and were validated by
 * backtesting on the 2018/2022 World Cups (see {@link Backtest} and {@link Tuner}).
 *
 * <p>Draws are modelled explicitly via {@link DrawModel} (Phase 1b.3).
 */
public final class EloRatingSystem {

    public static final double INITIAL_RATING = 1500.0;

    private final EloConfig config;
    private final Map<String, Double> ratings = new HashMap<>();
    private int matchesProcessed = 0;
    private int lastYearSeen = Integer.MIN_VALUE;

    public EloRatingSystem() {
        this(EloConfig.DEFAULT);
    }

    public EloRatingSystem(EloConfig config) {
        this.config = config;
    }

    /** Current rating of a team; unknown teams are at {@link #INITIAL_RATING}. */
    public double ratingOf(String team) {
        return ratings.getOrDefault(team, INITIAL_RATING);
    }

    /**
     * Expected score of player/team A against B: a value in (0, 1) where
     * 1 = certain win, 0.5 = even match. A 400-point rating gap means the
     * stronger team is expected to score ~0.91.
     */
    public static double expectedScore(double ratingA, double ratingB) {
        return 1.0 / (1.0 + Math.pow(10.0, (ratingB - ratingA) / 400.0));
    }

    /** Updates both teams' ratings based on a completed match. */
    public void processMatch(Match match) {
        applyAnnualRegression(match.date().getYear());

        double homeRating = ratingOf(match.homeTeam());
        double awayRating = ratingOf(match.awayTeam());

        double effectiveHome = homeRating
                + (match.neutralVenue() ? 0.0 : config.homeAdvantage());
        double expected = expectedScore(effectiveHome, awayRating);

        double actual = switch (match.outcome()) {
            case HOME_WIN -> 1.0;
            case DRAW -> 0.5;
            case AWAY_WIN -> 0.0;
        };

        double k = config.kFactor(match.tournament());
        if (config.goalMarginScaling()) {
            k *= marginMultiplier(match.homeScore() - match.awayScore());
        }

        double delta = k * (actual - expected);
        ratings.put(match.homeTeam(), homeRating + delta);
        ratings.put(match.awayTeam(), awayRating - delta);
        matchesProcessed++;
    }

    /**
     * Pulls every rating toward {@value #INITIAL_RATING} when the calendar year
     * advances, by {@code annualRegression} per elapsed year. Squads change;
     * results from long ago should count for less.
     */
    private void applyAnnualRegression(int year) {
        if (lastYearSeen != Integer.MIN_VALUE
                && year > lastYearSeen
                && config.annualRegression() > 0) {
            double keep = Math.pow(1.0 - config.annualRegression(), year - lastYearSeen);
            ratings.replaceAll((team, r) -> INITIAL_RATING + (r - INITIAL_RATING) * keep);
        }
        lastYearSeen = Math.max(lastYearSeen, year);
    }

    /**
     * Goal-margin weight from eloratings.net: 1-goal wins and draws count
     * normally; bigger margins move ratings further.
     */
    static double marginMultiplier(int goalDifference) {
        int margin = Math.abs(goalDifference);
        if (margin <= 1) return 1.0;
        if (margin == 2) return 1.5;
        return 1.75 + (margin - 3) / 8.0;
    }

    /**
     * Probability that {@code homeTeam} beats {@code awayTeam}.
     * Note: this is the Elo expected score, which treats a draw as half a win —
     * see the class-level TODO about modelling draws properly.
     */
    public double winProbability(String homeTeam, String awayTeam, boolean neutralVenue) {
        double home = ratingOf(homeTeam)
                + (neutralVenue ? 0.0 : config.homeAdvantage());
        return expectedScore(home, ratingOf(awayTeam));
    }

    /**
     * Explicit win/draw/loss probabilities for a matchup, using the empirical
     * draw model (see {@link DrawModel}).
     */
    public DrawModel.Probabilities outcomeProbabilities(String homeTeam, String awayTeam,
                                                        boolean neutralVenue) {
        double home = ratingOf(homeTeam)
                + (neutralVenue ? 0.0 : config.homeAdvantage());
        double away = ratingOf(awayTeam);
        return DrawModel.split(expectedScore(home, away), home - away);
    }

    /** Top {@code n} teams by current rating, strongest first. */
    public List<Entry<String, Double>> topRatings(int n) {
        return ratings.entrySet().stream()
                .sorted(Entry.<String, Double>comparingByValue().reversed())
                .limit(n)
                .toList();
    }

    public int matchesProcessed() {
        return matchesProcessed;
    }

    public int teamCount() {
        return ratings.size();
    }
}
