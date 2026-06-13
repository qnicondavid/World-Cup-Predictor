package com.david.worldcup.elo;

import com.david.worldcup.model.Match;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Grid search over {@link EloConfig} hyperparameters.
 *
 * <p>Methodology: every candidate config is scored by its match-weighted Brier
 * score pooled across the <b>2006, 2010, 2014 and 2018</b> World Cups (256
 * matches — the tuning set). The 2022 World Cup is held out and used once, to
 * validate the winner — tuning and reporting on the same data would be
 * overfitting.
 */
public final class Tuner {

    public record Candidate(EloConfig config, BacktestResult tuningResult) {}

    /** All candidates, best (lowest pooled tuning Brier) first. */
    public List<Candidate> search(List<Match> matches) {
        Backtest backtest = new Backtest();
        List<Candidate> candidates = new ArrayList<>();

        for (double kWorldCup : new double[] {40, 50, 60}) {
            for (double homeAdvantage : new double[] {50, 100}) {
                for (double kFriendly : new double[] {20, 30}) {
                    for (boolean margin : new boolean[] {false, true}) {
                        for (double regression : new double[] {0.0, 0.05, 0.10, 0.20}) {
                            EloConfig config = new EloConfig(
                                    kWorldCup, 40, 35, kFriendly,
                                    homeAdvantage, margin, regression);
                            candidates.add(new Candidate(config, backtest.runCombined(
                                    matches, Backtest.TUNING_WINDOWS, config)));
                        }
                    }
                }
            }
        }

        candidates.sort(Comparator.comparingDouble(c -> c.tuningResult().brierScore()));
        return candidates;
    }

    /** Held-out 2022 evaluation — call exactly once, on the chosen config. */
    public BacktestResult validate(List<Match> matches, EloConfig config) {
        Backtest.Window wc2022 = Backtest.WORLD_CUPS.get(Backtest.WORLD_CUPS.size() - 1);
        return new Backtest().run(matches, wc2022.from(), wc2022.until(), config);
    }
}
