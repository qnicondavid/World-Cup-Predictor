package com.david.worldcup.goals;

import com.david.worldcup.elo.DrawModel;

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
}
