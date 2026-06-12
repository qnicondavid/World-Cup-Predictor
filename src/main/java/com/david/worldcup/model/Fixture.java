package com.david.worldcup.model;

import java.time.LocalDate;

/** A scheduled match that has not been played yet (scores still "NA" in the CSV). */
public record Fixture(
        LocalDate date,
        String homeTeam,
        String awayTeam,
        String tournament,
        boolean neutralVenue) {

    public boolean isWorldCupFinals() {
        return "FIFA World Cup".equals(tournament);
    }
}
