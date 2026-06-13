package com.david.worldcup.goals;

import com.david.worldcup.elo.DrawModel;

import java.util.Optional;

/**
 * A model that predicts the win / draw / loss probabilities of a single match
 * by modelling the number of goals each side scores.
 *
 * <p>Unlike the online Elo rating system, goal models are <em>fit in batch</em>
 * on a block of historical matches and then queried; see {@link GoalModelBacktest}
 * for the train-before-the-tournament evaluation regime.
 */
public interface GoalModel {

    /** Win / draw / loss probabilities for {@code home} versus {@code away}. */
    DrawModel.Probabilities probabilities(String home, String away, boolean neutral);

    /** Short human-readable model name. */
    String name();

    /**
     * Expected goals for each side, when the model produces them. Outcome-only
     * models (e.g. the Elo + DrawModel baseline) return {@link Optional#empty()}.
     */
    default Optional<GoalRates> expectedGoals(String home, String away, boolean neutral) {
        return Optional.empty();
    }

    /** Expected goals for the home and away sides of a fixture. */
    record GoalRates(double home, double away) {}
}
