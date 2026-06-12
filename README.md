# World Cup Elo Predictor

An Elo-based prediction model for FIFA World Cup matches, trained on 150+ years
of international football results (49,000+ matches, 1872–present). Built during
the 2026 World Cup — predictions are scored against real results as the
tournament unfolds.

## How it works

Every national team starts at 1500 Elo. The model replays all of international
football history in date order; after each match, ratings shift by
`K * (actual − expected)`, where the expected score is a logistic function of
the rating gap. K scales with match importance (World Cup 60, qualifiers 40,
friendlies 20) and home teams get a +100 rating boost at non-neutral venues.

```
data/results.csv → MatchCsvParser → EloRatingSystem → rankings & predictions
```

## Run it

Requires JDK 17+ and Maven.

```bash
mvn test                            # run the unit test suite
mvn compile exec:java               # replay history, print Elo top 15 + sample predictions
mvn compile exec:java -Dexec.args="--backtest"   # evaluate on the 2018/2022 World Cups
mvn compile exec:java -Dexec.args="--tune"       # hyperparameter grid search
```

## Backtest results

The model is scored on World Cups it has never seen: it trains on all matches
before the tournament, then predicts each match *before* learning from its
result — the same information regime as predicting in real time.

| Tournament | Baseline | Tuned + margin scaling |
|---|---|---|
| World Cup 2018 | 34/64 (53.1%), Brier 0.167 | 36/64 (56.3%), Brier 0.159 |
| World Cup 2022 | 34/64 (53.1%), Brier 0.181 | 34/64 (53.1%), Brier 0.180 |
| Coin-flip reference | 50%, Brier 0.250 | — |

The tuned config (K_worldcup 40, home advantage 50, K_friendly 30, goal-margin
scaling on) was selected by grid search on 2018 only, then validated once on
held-out 2022 to avoid overfitting. Interesting findings: a *lower* home
advantage and *higher* friendly weight than the eloratings.net folklore values,
and goal-margin scaling helps on both tournaments.

Draws (~20% of World Cup matches) always count as misses since the model only
predicts win/loss — explicit draw modelling is on the roadmap.

## Roadmap

- [x] **Phase 1** — Data ingestion, Elo engine, unit tests
- [x] **Phase 1b.1** — Backtest harness (accuracy + Brier score on 2018/2022)
- [x] **Phase 1b.2** — Goal-margin K scaling + hyperparameter grid search
- [ ] **Phase 1b.3** — Explicit draw modelling
- [ ] **Phase 2** — Live 2026 tracker: GitHub Action fetches results, scores my
      predictions, auto-updates the accuracy table below
- [ ] **Phase 3** — Monte Carlo simulation of the remaining bracket (10,000 runs)
- [ ] **Phase 4** — Spring Boot REST API serving predictions

## 2026 prediction accuracy

_Coming in Phase 2 — this table will update automatically after every matchday._

## Data

Match data from [martj42/international_results](https://github.com/martj42/international_results)
(includes scheduled 2026 fixtures, used as the prediction list in Phase 2).
