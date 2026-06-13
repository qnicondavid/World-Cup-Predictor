package com.david.worldcup.goals;

import com.david.worldcup.elo.DrawModel;
import com.david.worldcup.model.Match;
import com.david.worldcup.value.MarketValueTable;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Dixon-Coles goal model: independent Poisson goal counts from fitted
 * attack/defence ratings, plus the low-score correlation correction.
 */
public final class DixonColesModel implements GoalModel {

    private final TeamStrength strength;

    public DixonColesModel(TeamStrength strength) {
        this.strength = strength;
    }

    /** Fits the model on a block of historical matches as of {@code asof}. */
    public static DixonColesModel fit(List<Match> training, LocalDate asof) {
        return new DixonColesModel(new PoissonRatingsFitter().fit(training, asof));
    }

    /**
     * Fits the model, then folds in squad market value as a prior on the
     * attack/defence ratings (see {@link ValueAdjuster}). Falls back to the plain
     * fit when the value table is empty or has no data as of {@code asof}.
     */
    public static DixonColesModel fitWithValues(List<Match> training, LocalDate asof,
                                                MarketValueTable values, ValueWeights weights) {
        TeamStrength base = new PoissonRatingsFitter().fit(training, asof);
        Map<String, Integer> matchCounts = new HashMap<>();
        for (Match m : training) {
            matchCounts.merge(m.homeTeam(), 1, Integer::sum);
            matchCounts.merge(m.awayTeam(), 1, Integer::sum);
        }
        return new DixonColesModel(ValueAdjuster.adjust(base, matchCounts, values, asof, weights));
    }

    public TeamStrength strength() {
        return strength;
    }

    @Override
    public DrawModel.Probabilities probabilities(String home, String away, boolean neutral) {
        double lh = strength.lambdaHome(home, away, neutral);
        double la = strength.lambdaAway(home, away, neutral);
        return ScoreGrid.dixonColes(lh, la, strength.rho());
    }

    @Override
    public Optional<GoalRates> expectedGoals(String home, String away, boolean neutral) {
        return Optional.of(new GoalRates(
                strength.lambdaHome(home, away, neutral),
                strength.lambdaAway(home, away, neutral)));
    }

    @Override
    public String name() {
        return "Dixon-Coles";
    }
}
