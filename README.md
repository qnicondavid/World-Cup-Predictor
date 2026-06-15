# World Cup Elo Predictor

![CI](https://github.com/qnicondavid/World-Cup-Predictor/actions/workflows/ci.yml/badge.svg)

**▶ Live demo: [qnicondavid.github.io/World-Cup-Predictor](https://qnicondavid.github.io/World-Cup-Predictor/)** —
daily-updated 2026 predictions, scored record, and Monte Carlo championship odds.

A prediction model for FIFA World Cup matches, trained on 150+ years of
international football results (49,000+ matches, 1872–present). It began as an
Elo rating system and now serves predictions from a **Dixon-Coles goal model**
with a squad market-value prior; everything is evaluated on held-out World Cups
and the live 2026 predictions are scored against real results as the tournament
unfolds.

## 2026 prediction accuracy (live)

A GitHub Action runs daily: it pulls fresh results, locks predictions for
upcoming fixtures with the **Dixon-Coles goal model** (the best performer in the
held-out comparison below, with squad market value folded in as a prior when
available), scores completed ones, and updates this section automatically.
Predictions locked earlier under the Elo model are preserved unchanged — a
prediction is never re-locked once it has been made.

<!-- TRACKER:START -->
_Updated 2026-06-15 — predictions are locked before kickoff and never edited; the git history of `predictions/predictions.csv` is the proof. Each pick is the model's most likely outcome and the H/D/A column its full home-win / draw / away-win split; the predicted score is the most likely scoreline (expected goals in brackets), and Δ is the total goal difference from the actual result (🎯 = exact). Brier is multiclass._

**Record: 4/10 picks correct (40.0%) — multiclass Brier 0.770 — mean goal error 2.3** (uniform guess = 0.667)

| Date | Match | Pick | H/D/A | Pred (xG) | Result | Δ | Hit |
|---|---|---|---|---|---|---|---|
| Jun 12 | Canada vs Bosnia and Herzegovina | Canada | 75/18/7% | 2-0 (2.7–0.6) | 1-1 | 2 | ❌ |
| Jun 12 | United States vs Paraguay | United States | 36/30/34% | 1-1 (1.3–1.3) | 4-1 | 3 | ✅ |
| Jun 13 | Qatar vs Switzerland | Switzerland | 3/12/85% | 0-3 (0.5–3.6) | 1-1 | 3 | ❌ |
| Jun 13 | Brazil vs Morocco | Brazil | 46/28/26% | 1-1 (1.5–1.1) | 1-1 | 0 🎯 | ❌ |
| Jun 13 | Haiti vs Scotland | Scotland | 15/25/60% | 0-1 (0.8–2.0) | 0-1 | 0 🎯 | ✅ |
| Jun 13 | Australia vs Turkey | Turkey | 25/28/48% | 1-1 (1.1–1.6) | 2-0 | 2 | ❌ |
| Jun 14 | Germany vs Curaçao | Germany | 88/10/2% | 4-0 (4.0–0.4) | 7-1 | 4 | ✅ |
| Jun 14 | Ivory Coast vs Ecuador | Ecuador | 14/25/61% | 0-2 (0.8–2.0) | 1-0 | 3 | ❌ |
| Jun 14 | Netherlands vs Japan | Netherlands | 36/30/35% | 1-1 (1.3–1.3) | 2-2 | 2 | ❌ |
| Jun 14 | Sweden vs Tunisia | Sweden | 42/29/29% | 1-1 (1.5–1.2) | 5-1 | 4 | ✅ |

**Locked for upcoming matches:**

| Date | Match | Pick | H/D/A | Pred (xG) |
|---|---|---|---|---|
| Jun 15 | Belgium vs Egypt | Belgium | 63/24/13% | 2-0 (2.1–0.8) |
| Jun 15 | Iran vs New Zealand | Iran | 65/24/11% | 2-0 (2.2–0.8) |
| Jun 15 | Spain vs Cape Verde | Spain | 93/5/2% | 5-0 (5.0–0.3) |
| Jun 15 | Saudi Arabia vs Uruguay | Uruguay | 8/20/72% | 0-2 (0.7–2.5) |
| Jun 16 | France vs Senegal | France | 66/23/11% | 2-0 (2.2–0.8) |
| Jun 16 | Iraq vs Norway | Norway | 9/20/71% | 0-2 (0.7–2.5) |
| Jun 16 | Argentina vs Algeria | Argentina | 72/19/8% | 2-0 (2.5–0.7) |
| Jun 16 | Austria vs Jordan | Austria | 60/25/15% | 1-0 (2.0–0.9) |
| Jun 17 | Portugal vs DR Congo | Portugal | 76/18/6% | 2-0 (2.8–0.6) |
| Jun 17 | Uzbekistan vs Colombia | Colombia | 10/21/69% | 0-2 (0.7–2.3) |

<!-- TRACKER:END -->

### Before the model existed (retrospective)

The 2026 World Cup had already kicked off when this model was built. These early
matches were never locked, so they are shown separately and excluded from the
record above — each is a retrospective prediction trained only on data from
before that match.

<!-- EARLY:START -->
_These were played **before the model existed**, so they were never locked. Each row is a *retrospective* prediction, computed by training only on data from before that match — never peeking at the result — and is **not counted** in the record above. Shown for a complete tournament picture._

| Date | Match | Pick | H/D/A | Pred (xG) | Result | Δ | Hit |
|---|---|---|---|---|---|---|---|
| Jun 11 | Mexico vs South Africa | Mexico | 63/24/13% | 1-0 (1.8–0.7) | 2-0 | 1 | ✅ |
| Jun 11 | South Korea vs Czech Republic | South Korea | 40/28/32% | 1-1 (1.3–1.1) | 2-1 | 1 | ✅ |

<!-- EARLY:END -->

### Championship odds (live)

The model's title picks from 10,000 Monte Carlo simulations of the rest of the
tournament, refreshed daily by the same Action.

<!-- TITLE:START -->
_The model's championship odds from 10,000 Monte Carlo simulations, updated 2026-06-15. They inherit the simulator's simplifications (Elo tie-breaks, seeded knockout pairings, knockout games as neutral with no draws), so read them as the model's view, not a hard forecast._

| # | Team | Title | Final | Semis |
|---|---|---|---|---|
| 1 | Spain | 22.3% | 34.4% | 48.9% |
| 2 | Argentina | 18.9% | 29.8% | 44.4% |
| 3 | France | 10.8% | 19.7% | 33.5% |
| 4 | England | 6.4% | 13.1% | 26.0% |
| 5 | Brazil | 5.4% | 11.3% | 21.9% |
| 6 | Colombia | 4.8% | 10.1% | 20.4% |
| 7 | Portugal | 4.6% | 9.7% | 20.2% |
| 8 | Germany | 4.3% | 9.6% | 19.6% |
| 9 | Netherlands | 3.1% | 7.0% | 14.8% |
| 10 | Japan | 2.8% | 6.6% | 14.5% |
| 11 | Morocco | 2.3% | 5.5% | 13.1% |
| 12 | Belgium | 2.1% | 5.4% | 12.4% |
| 13 | Mexico | 1.7% | 4.6% | 11.9% |
| 14 | Norway | 1.5% | 4.0% | 10.1% |
| 15 | Croatia | 1.4% | 3.7% | 9.9% |
| 16 | Ecuador | 1.0% | 2.9% | 7.8% |

<!-- TITLE:END -->

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
mvn compile exec:java -Dexec.args="--track"      # lock/score predictions, update README
mvn compile exec:java -Dexec.args="--simulate"   # Monte Carlo: 10,000 tournament sims
mvn compile exec:java -Dexec.args="--upcoming"   # every fixture with win/draw/loss probs
mvn compile exec:java -Dexec.args="--predict=France,Argentina"   # any matchup
mvn compile exec:java -Dexec.args="--goals"      # goal models vs Elo: held-out comparison
mvn compile exec:java -Dexec.args="--rest"       # does a rest-days edge improve the model?
mvn compile exec:java -Dexec.args="--values"     # does squad market value improve the model?
mvn compile exec:java -Dexec.args="--values-tune" # grid-search the market-value prior weights
mvn compile exec:java -Dexec.args="--calibrate"  # reliability / log-loss audit + temperature fit
mvn compile exec:java -Dexec.args="--bets"       # value bets vs bookmaker odds (mock odds)
```

(PowerShell: quote the whole flag, e.g. `mvn compile exec:java "-Dexec.args=--simulate"`.)

## Backtest results

The model is scored on five World Cups it has never seen: for each tournament it
trains only on matches played before it, then predicts each match *before*
learning its result — the same information regime as predicting in real time.

| Tournament | Tuned model | Baseline |
|---|---|---|
| World Cup 2006 | 41/64 (64.1%), Brier 0.119 | 42/64 (65.6%), Brier 0.129 |
| World Cup 2010 | 35/64 (54.7%), Brier 0.146 | 32/64 (50.0%), Brier 0.148 |
| World Cup 2014 | 39/64 (60.9%), Brier 0.135 | 39/64 (60.9%), Brier 0.150 |
| World Cup 2018 | 37/64 (57.8%), Brier 0.159 | 34/64 (53.1%), Brier 0.167 |
| World Cup 2022 | 32/64 (50.0%), Brier 0.183 | 34/64 (53.1%), Brier 0.181 |
| **Combined (320)** | **184/320 (57.5%), Brier 0.148** | 181/320 (56.6%), Brier 0.155 |
| Coin-flip reference | 50%, Brier 0.250 | — |

The tuned config (K_worldcup 60, home advantage 50, K_friendly 30, goal-margin
scaling on) was selected by pooled grid search over 2006-2018, then validated
once on held-out 2022. Findings worth noting:

- **Goal-margin scaling helps consistently** (combined Brier 0.148 vs 0.155).
- **Annual regression toward the mean never helped** at any strength tested —
  national-team strength is more persistent than folklore suggests.
- **World Cups are getting harder to predict**: Brier rises almost monotonically
  from 0.119 (2006) to 0.183 (2022). The field has genuinely tightened.

**Three-way scoring** (win/draw/loss via the draw model): multiclass Brier of
0.50-0.62 across the five tournaments against 0.667 for uniform guessing. An
honest finding: the model never makes "draw" its single most likely outcome
(~30% is the ceiling), so three-way pick accuracy equals binary accuracy — the
draw model improves *probabilities*, not *picks*. Bookmakers share this
property: the draw is almost never the favorite.

## Draw modelling

The Elo expected score conflates winning and drawing (E = P(win) + P(draw)/2).
To split it, P(draw) is estimated empirically: replaying 37,314 internationals
since 1980 through the model shows the draw rate falling from ~30% between
equal teams to ~2% at a 600-point rating gap. `DrawModel` interpolates that
observed curve and splits E into explicit win/draw/loss probabilities.

## Tournament simulation

`--simulate` runs 10,000 Monte Carlo tournaments: replays the real group
results so far, samples every remaining fixture from the model's win/draw/loss
probabilities, resolves groups (winners, runners-up, best thirds), then samples
the knockout bracket to a champion. Documented simplifications: Elo tie-breaks
instead of goal difference, seeded knockout pairings, and knockout draws folded
into the win probability.

## Goal models (experimental)

Elo rates a team on a single axis. A goal model instead gives every team a
separate **attack** and **defence** rating and predicts the *scoreline*, which
yields win/draw/loss probabilities from first principles rather than from an
empirical draw curve. This is the standard approach for forecasts that aim to
beat the bookmaker, so the repo now carries three of them, all under
`com.david.worldcup.goals` and comparable head-to-head via `--goals`:

- **Dixon-Coles** — independent Poisson goals from fitted attack/defence ratings,
  with a home-advantage term, the low-score correlation correction (`rho`) that
  fixes Poisson's under-counting of 0-0 and 1-1, exponential **time decay**
  (2-year half-life — squads turn over) and shrinkage toward the average for
  rarely-seen teams. Fit by weighted maximum likelihood via iterative scaling.
- **Bivariate Poisson** — the same attack/defence fit plus a shared component
  that makes the two scores positively correlated.
- **Elo-Poisson** — the lightweight option: reuse the existing Elo gap and map
  it to two Poisson rates by regression. Keeps the tuned Elo engine; weaker.

Each is evaluated the same honest way as Elo — fit only on the 12 years before a
World Cup, then predict every finals match — and scored by multiclass Brier
against the **Elo + DrawModel** baseline.

### Held-out comparison

Scored on 320 World Cup matches (2006-2022), train-before-each-tournament:

| Model | Picks correct | Combined multiclass Brier |
|---|---|---|
| Dixon-Coles | 183/320 (57.2%) | 0.574 |
| Bivariate Poisson | 183/320 (57.2%) | 0.573 |
| Elo-Poisson | 178/320 (55.6%) | 0.575 |
| Elo + DrawModel (baseline) | 178/320 (55.6%) | 0.576 |
| Uniform reference | — | 0.667 |

The goal models edge the Elo baseline on both metrics, but modestly — five more
correct picks and ~0.003 of Brier. Findings:

- **The bivariate covariance term adds nothing** over Dixon-Coles (0.573 vs
  0.574), the usual result for football. Dixon-Coles is the goal model to keep.
- **The edge is uneven**: Dixon-Coles is clearly better in 2006 (0.525 vs 0.538)
  and 2018 (0.576 vs 0.596), while Elo wins 2014 and 2022. They capture different
  things, so an **Elo + Dixon-Coles ensemble** (averaging the two probability
  vectors) is also wired into `--goals`; averaging comparable-but-different
  models often beats either alone. Re-run `--goals` to see its row.
- A Brier near 0.57 is roughly bookmaker territory (~0.55-0.56 on World Cup
  matches) but not yet clearly beating the closing line — and that cannot be
  confirmed without an odds feed.

The fitter itself is validated against `research/goal_models.py`: on data from a
known Dixon-Coles process it recovers team attack strengths at correlation 0.99.

**Toward beating the bookmaker.** Calibration comes first — a model only has
*value* if its probabilities are better than the closing line. Once a model wins
on held-out Brier, the betting layer is a small add-on: convert odds to implied
probabilities (remove the overround), bet when the model's probability exceeds
the implied one by a margin, and size with fractional Kelly. That step needs a
historical-odds feed, which the current dataset does not include.

## Squad market value

Elo and goal ratings are *lagging* — they learn a team's strength from results.
Squad market value is a *leading* signal: it reflects current player quality
directly. The model folds it in as a **prior on the Dixon-Coles attack/defence
ratings**: a richer-than-average squad gets a higher attack and lower (better)
defence prior, and each team's fitted rating is shrunk toward that prior. Two
levers control the shrinkage (`ValueWeights`): a uniform `globalWeight` applied
to every team, and a `sparseWeight` that adds extra pull for teams with little
match data. Tuning (below) found the sparse lever unnecessary, so in practice it
is the global blend that does the work.

The model reads `data/market_values.csv` with columns `team,as_of,value_eur`.
Multiple dated rows per team are allowed, and lookups always take the most
recent value *on or before* the match date, so nothing leaks from the future.
When the file is absent the model behaves exactly as before. A small
**illustrative** sample ships in the repo; replace it with real data.

**Getting real data.** Transfermarkt has no public API, so use the community
datasets (download them where you have network access, not from CI):

- [dcaribou/transfermarkt-datasets](https://github.com/dcaribou/transfermarkt-datasets)
  — the best fit: a `player_valuations` table with *dated* market values plus
  national-team data, refreshed weekly. Aggregate player valuations to a squad
  total per national team per date to build `market_values.csv`.
- [salimt/football-datasets](https://github.com/salimt/football-datasets) and the
  Kaggle mirror [davidcariboo/player-scores](https://www.kaggle.com/datasets/davidcariboo/player-scores)
  are alternatives.

**Does it help? (tuned, held-out)** `--values-tune` grid-searches the weights on
World Cups 2006-2018 and validates once on held-out 2022. The tuned prior
(`globalWeight 0.20, sparseWeight 0, valueScale 0.30`) beats plain Dixon-Coles
out of sample — multiclass Brier **0.6065 vs 0.6123** on 2022, and **0.5629 vs
0.5650** in-sample — so it is now the default. Caveats kept in view: the gain is
small (~0.006 Brier, identical 33/64 pick accuracy), it rests on a single
held-out tournament, and the winning weights sit at the edge of the search grid,
so a wider sweep may do a little better. With only a *current* value snapshot
the historical rows are unchanged (no value existed then); the verdict above
needs the historical snapshots that `research/build_market_values.py` produces.

At default weights the untuned prior was a wash (Brier 0.575 vs 0.574); the
improvement comes only after tuning, and only from the global blend — the
sparse-team lever, intuitive as it was, earned nothing at World Cup level.

## Value betting (toward beating the book)

A well-calibrated model only has *value* if it disagrees with the market. `--bets`
closes that loop: it reads bookmaker odds from `data/odds_sample.csv`
(`match_date,home_team,away_team,home_odds,draw_odds,away_odds`, decimal), strips
the overround to fair probabilities, and for each upcoming fixture compares the
production model's win/draw/loss probabilities to the price. When an outcome's
expected value (`model_probability × odds − 1`) clears a threshold it is flagged
as a bet and sized by fractional Kelly.

The default policy (`BettingConfig`) is deliberately conservative — a 5% edge
floor, quarter-Kelly, capped at 5% of bankroll — because the calibration audit
showed the model's confidence drifts between tournaments, so thin edges are not
worth chasing. `ValueBetting.settle` scores a bet against the actual result, the
building block for forward-tracked ROI.

The shipped odds file is **mock** data for demonstration. For real use, wire a
live feed (e.g. The Odds API free tier) into an `OddsTable` and, to validate
honestly, lock each flagged bet at its pre-kickoff price in the ledger and track
ROI forward — the same never-edited, git-proven discipline as the predictions.
Historical international closing odds barely exist, so forward-testing, not
backtesting, is the credible path here.

## Calibration

`--calibrate` audits the production model on the held-out World Cups: reliability
bins (predicted vs. observed frequency), log-loss, multiclass Brier and expected
calibration error (ECE), plus a temperature fit (tuned on 2006-2018, validated on
2022). The finding: the model is mildly **under-confident** — favorites win a bit
more often than it predicts and longshots a bit less (ECE ≈ 0.06). A fitted
temperature would sharpen the probabilities, and it helps in-sample — but it
makes **held-out 2022 worse on every metric** (the upset-heavy tournament punished
extra confidence in favorites). The calibration direction is not stable across
tournaments, so no temperature is applied: the raw probabilities ship as-is. The
practical consequence for any betting layer is to demand a margin of safety and
size conservatively, since the edge you compute rests on calibration that wobbles
year to year.

## Rest-days differential (experimental)

A team that comes into a match with more recovery than its opponent should have
a small edge the rating alone misses. `--rest` tests exactly that: replaying in
date order, it tracks each team's previous match and adds rating points per day
of rest advantage (capped at 10 days, so a long pre-tournament break is not
mistaken for a huge edge), then measures held-out multiclass Brier against the
unadjusted baseline — fitting the points-per-day on 2006-2018 and validating on
2022. It needs no new data (match dates are already in `results.csv`), so it is
fully reproducible. Run `--rest` to see whether the effect survives out of
sample; within a World Cup the schedule is fairly even, so expect the signal to
be modest.

## Data

Match data from [martj42/international_results](https://github.com/martj42/international_results)
(includes scheduled 2026 fixtures, used as the prediction list in Phase 2).
