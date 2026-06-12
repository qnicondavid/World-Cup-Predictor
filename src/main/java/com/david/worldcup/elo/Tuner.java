package com.david.worldcup.elo;

import com.david.worldcup.model.Match;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Grid search over {@link EloConfig} hyperparameters.
 *
 * <p>Methodology: every candidate config is scored by Brier on the <b>2018</b>
 * World Cup only (the tuning set). The 2022 World Cup is held out and used once,
 * to validate the winner — tuning and reporting on the same data would be
 * overfitting.
 */
public final class Tuner {

    private static final LocalDate TUNE_FROM = LocalDate.of(2018, 6, 1);
    private static final LocalDate TUNE_UNTIL = LocalDate.of(2018, 7, 31);
    private static final LocalDate VALIDATE_FROM = LocalDate.of(2022, 11, 1);
    private static final LocalDate VALIDATE_UNTIL = LocalDate.of(2022, 12, 31);

    public record Candidate(EloConfig config, BacktestResult tuningResult) {}

    /** All candidates, best (lowest tuning Brier) first. */
    public List<Candidate> search(List<Match> matches) {
        Backtest backtest = new Backtest();
        List<Candidate> candidates = new ArrayList<>();

        for (double kWorldCup : new double[] {40, 50, 60}) {
            for (double homeAdvantage : new double[] {50, 100, 150}) {
                for (double kFriendly : new double[] {10, 20, 30}) {
                    for (boolean margin : new boolean[] {false, true}) {
                        EloConfig config = new EloConfig(
                                kWorldCup, 40, 35, kFriendly, homeAdvantage, margin);
                        BacktestResult result = backtest.run(
                                matches, TUNE_FROM, TUNE_UNTIL, config);
                        candidates.add(new Candidate(config, result));
                    }
                }
            }
        }

        candidates.sort(Comparator.comparingDouble(c -> c.tuningResult().brierScore()));
        return candidates;
    }

    /** Held-out 2022 evaluation — call exactly once, on the chosen config. */
    public BacktestResult validate(List<Match> matches, EloConfig config) {
        return new Backtest().run(matches, VALIDATE_FROM, VALIDATE_UNTIL, config);
    }
}
