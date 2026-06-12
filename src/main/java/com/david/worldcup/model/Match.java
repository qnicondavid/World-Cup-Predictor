package com.david.worldcup.model;

import java.time.LocalDate;

/**
 * A single completed international match.
 *
 * @param date         date the match was played
 * @param homeTeam     name of the home team (or first-listed team on neutral ground)
 * @param awayTeam     name of the away team
 * @param homeScore    full-time goals for the home team
 * @param awayScore    full-time goals for the away team
 * @param tournament   competition name, e.g. "FIFA World Cup", "Friendly"
 * @param neutralVenue true if the match was played at a neutral venue
 */
public record Match(
        LocalDate date,
        String homeTeam,
        String awayTeam,
        int homeScore,
        int awayScore,
        String tournament,
        boolean neutralVenue) {

    public enum Outcome { HOME_WIN, DRAW, AWAY_WIN }

    public Outcome outcome() {
        if (homeScore > awayScore) return Outcome.HOME_WIN;
        if (homeScore < awayScore) return Outcome.AWAY_WIN;
        return Outcome.DRAW;
    }

    public boolean isWorldCupFinals() {
        return "FIFA World Cup".equals(tournament);
    }
}
