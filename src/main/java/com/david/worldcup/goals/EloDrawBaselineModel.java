package com.david.worldcup.goals;

import com.david.worldcup.elo.DrawModel;
import com.david.worldcup.elo.EloRatingSystem;
import com.david.worldcup.model.Match;

import java.util.Comparator;
import java.util.List;

/**
 * The incumbent, wrapped as a {@link GoalModel} so it can be compared head to
 * head with the goal models on the same batch-trained footing: the tuned Elo
 * rating system plus the empirical {@link DrawModel} for the win/draw/loss split.
 */
public final class EloDrawBaselineModel implements GoalModel {

    private final EloRatingSystem elo;

    public EloDrawBaselineModel(EloRatingSystem elo) {
        this.elo = elo;
    }

    public static EloDrawBaselineModel fit(List<Match> training) {
        EloRatingSystem elo = new EloRatingSystem();
        training.stream()
                .sorted(Comparator.comparing(Match::date))
                .forEach(elo::processMatch);
        return new EloDrawBaselineModel(elo);
    }

    @Override
    public DrawModel.Probabilities probabilities(String home, String away, boolean neutral) {
        return elo.outcomeProbabilities(home, away, neutral);
    }

    @Override
    public String name() {
        return "Elo + DrawModel";
    }
}
