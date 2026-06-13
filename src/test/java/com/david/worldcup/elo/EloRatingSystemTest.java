package com.david.worldcup.elo;

import com.david.worldcup.model.Match;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EloRatingSystemTest {

    private static final double EPSILON = 1e-9;

    private static Match match(String home, String away, int homeGoals, int awayGoals,
                               String tournament) {
        return new Match(LocalDate.of(2022, 11, 21), home, away,
                homeGoals, awayGoals, tournament, true);
    }

    @Test
    void evenlyMatchedTeamsHaveFiftyPercentExpectedScore() {
        assertEquals(0.5, EloRatingSystem.expectedScore(1500, 1500), EPSILON);
    }

    @Test
    void expectedScoresOfBothSidesSumToOne() {
        double a = EloRatingSystem.expectedScore(1700, 1450);
        double b = EloRatingSystem.expectedScore(1450, 1700);
        assertEquals(1.0, a + b, EPSILON);
    }

    @Test
    void fourHundredPointGapMeansRoughlyNinetyPercent() {
        double expected = EloRatingSystem.expectedScore(1900, 1500);
        assertTrue(expected > 0.90 && expected < 0.92,
                "got " + expected);
    }

    @Test
    void unknownTeamStartsAtInitialRating() {
        EloRatingSystem elo = new EloRatingSystem();
        assertEquals(EloRatingSystem.INITIAL_RATING, elo.ratingOf("Atlantis"), EPSILON);
    }

    @Test
    void winnerGainsAndLoserLosesEqually() {
        EloRatingSystem elo = new EloRatingSystem();
        elo.processMatch(match("Brazil", "Germany", 2, 0, "FIFA World Cup"));

        double brazilGain = elo.ratingOf("Brazil") - EloRatingSystem.INITIAL_RATING;
        double germanyLoss = EloRatingSystem.INITIAL_RATING - elo.ratingOf("Germany");

        assertTrue(brazilGain > 0, "winner should gain rating");
        assertEquals(brazilGain, germanyLoss, EPSILON); // zero-sum
    }

    @Test
    void upsetWinPaysMoreThanExpectedWin() {
        // Build a strong team by feeding it wins first.
        EloRatingSystem elo = new EloRatingSystem();
        for (int i = 0; i < 10; i++) {
            elo.processMatch(match("Giants", "Minnows", 3, 0, "FIFA World Cup"));
        }
        double giantsBefore = elo.ratingOf("Giants");
        double minnowsBefore = elo.ratingOf("Minnows");

        // Now the minnows pull off the upset.
        elo.processMatch(match("Minnows", "Giants", 1, 0, "FIFA World Cup"));
        double upsetGain = elo.ratingOf("Minnows") - minnowsBefore;

        // Compare to what the giants earned for their last routine win.
        EloRatingSystem reference = new EloRatingSystem();
        reference.processMatch(match("A", "B", 1, 0, "FIFA World Cup"));
        double routineGain = reference.ratingOf("A") - EloRatingSystem.INITIAL_RATING;

        assertTrue(upsetGain > routineGain,
                "upset gain " + upsetGain + " should exceed routine gain " + routineGain);
        assertTrue(giantsBefore > minnowsBefore, "sanity: giants were stronger");
    }

    @Test
    void worldCupMatchesMoveRatingsMoreThanFriendlies() {
        EloRatingSystem worldCup = new EloRatingSystem();
        worldCup.processMatch(match("A", "B", 1, 0, "FIFA World Cup"));

        EloRatingSystem friendly = new EloRatingSystem();
        friendly.processMatch(match("A", "B", 1, 0, "Friendly"));

        double worldCupSwing = worldCup.ratingOf("A") - EloRatingSystem.INITIAL_RATING;
        double friendlySwing = friendly.ratingOf("A") - EloRatingSystem.INITIAL_RATING;
        assertTrue(worldCupSwing > friendlySwing);
    }

    @Test
    void homeAdvantageRaisesHomeWinProbability() {
        EloRatingSystem elo = new EloRatingSystem();
        double neutral = elo.winProbability("A", "B", true);
        double atHome = elo.winProbability("A", "B", false);
        assertEquals(0.5, neutral, EPSILON);
        assertTrue(atHome > neutral);
    }

    @Test
    void marginMultiplierFollowsEloRatingsScheme() {
        assertEquals(1.0, EloRatingSystem.marginMultiplier(0), EPSILON);
        assertEquals(1.0, EloRatingSystem.marginMultiplier(1), EPSILON);
        assertEquals(1.5, EloRatingSystem.marginMultiplier(-2), EPSILON); // sign-agnostic
        assertEquals(1.75, EloRatingSystem.marginMultiplier(3), EPSILON);
        assertEquals(1.875, EloRatingSystem.marginMultiplier(4), EPSILON);
    }

    @Test
    void biggerWinsMoveRatingsFurtherWhenMarginScalingIsOn() {
        EloConfig margin = new EloConfig(60, 40, 35, 20, 100, true, 0.0);

        EloRatingSystem narrow = new EloRatingSystem(margin);
        narrow.processMatch(match("A", "B", 1, 0, "FIFA World Cup"));

        EloRatingSystem thrashing = new EloRatingSystem(margin);
        thrashing.processMatch(match("A", "B", 5, 0, "FIFA World Cup"));

        assertTrue(thrashing.ratingOf("A") > narrow.ratingOf("A"));
    }

    @Test
    void homeAdvantageIsConfigurable() {
        EloRatingSystem noHomeEdge = new EloRatingSystem(
                new EloConfig(60, 40, 35, 20, 0, false, 0.0));
        assertEquals(0.5, noHomeEdge.winProbability("A", "B", false), EPSILON);
    }

    @Test
    void annualRegressionPullsRatingsTowardTheMean() {
        EloConfig config = new EloConfig(60, 40, 35, 20, 0, false, 0.5);
        EloRatingSystem elo = new EloRatingSystem(config);

        elo.processMatch(new Match(LocalDate.of(2020, 6, 1), "A", "B",
                1, 0, "FIFA World Cup", true));
        double gain = elo.ratingOf("A") - EloRatingSystem.INITIAL_RATING;

        // A year passes: the next processed match triggers 50% regression.
        elo.processMatch(new Match(LocalDate.of(2021, 6, 1), "C", "D",
                1, 0, "FIFA World Cup", true));

        assertEquals(EloRatingSystem.INITIAL_RATING + gain * 0.5,
                elo.ratingOf("A"), EPSILON);
    }

    @Test
    void zeroRegressionLeavesRatingsUntouchedAcrossYears() {
        EloRatingSystem elo = new EloRatingSystem(EloConfig.BASELINE);
        elo.processMatch(new Match(LocalDate.of(2020, 6, 1), "A", "B",
                1, 0, "FIFA World Cup", true));
        double after2020 = elo.ratingOf("A");
        elo.processMatch(new Match(LocalDate.of(2023, 6, 1), "C", "D",
                1, 0, "FIFA World Cup", true));
        assertEquals(after2020, elo.ratingOf("A"), EPSILON);
    }

    @Test
    void drawAgainstEqualOpponentOnNeutralGroundChangesNothing() {
        EloRatingSystem elo = new EloRatingSystem();
        elo.processMatch(match("A", "B", 1, 1, "FIFA World Cup"));
        assertEquals(EloRatingSystem.INITIAL_RATING, elo.ratingOf("A"), EPSILON);
        assertEquals(EloRatingSystem.INITIAL_RATING, elo.ratingOf("B"), EPSILON);
    }
}
