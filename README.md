# World Cup Elo Predictor

![CI](https://github.com/qnicondavid/World-Cup-Predictor/actions/workflows/ci.yml/badge.svg)

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
mvn compile exec:java -Dexec.args="--track"      # lock/score predictions, update README
mvn compile exec:java -Dexec.args="--simulate"   # Monte Carlo: 10,000 tournament sims
mvn compile exec:java -Dexec.args="--upcoming"   # every fixture with win/draw/loss probs
mvn compile exec:java -Dexec.args="--predict=France,Argentina"   # any matchup
mvn compile exec:java -Dexec.args="--goals"      # goal models vs Elo: held-out comparison
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

## 2026 prediction accuracy (live)

A GitHub Action runs daily: it pulls fresh results, locks predictions for
upcoming fixtures using current ratings, scores completed ones, and updates
this section automatically.

<!-- TRACKER:START -->
_Updated 2026-06-13 — predictions are locked before kickoff and never edited; the git history of `predictions/predictions.csv` is the proof. Each pick is the model's most likely outcome; the H/D/A column is its full home-win / draw / away-win split, and the Brier score is multiclass._

**Record: 1/2 picks correct (50.0%) — multiclass Brier 0.925** (uniform guess = 0.667)

| Date | Match | Pick | H/D/A | Result | Hit |
|---|---|---|---|---|---|
| Jun 12 | Canada vs Bosnia and Herzegovina | Canada | 75/18/7% | 1-1 | ❌ |
| Jun 12 | United States vs Paraguay | United States | 36/30/34% | 4-1 | ✅ |

**Locked for upcoming matches:**

| Date | Match | Pick | H/D/A |
|---|---|---|---|
| Jun 13 | Qatar vs Switzerland | Switzerland | 3/12/85% |
| Jun 13 | Brazil vs Morocco | Brazil | 46/28/26% |
| Jun 13 | Haiti vs Scotland | Scotland | 15/25/60% |
| Jun 13 | Australia vs Turkey | Turkey | 25/28/48% |
| Jun 14 | Germany vs Curaçao | Germany | 88/10/2% |
| Jun 14 | Ivory Coast vs Ecuador | Ecuador | 14/25/61% |
| Jun 14 | Netherlands vs Japan | Netherlands | 36/30/35% |
| Jun 14 | Sweden vs Tunisia | Sweden | 42/29/29% |
| Jun 15 | Belgium vs Egypt | Belgium | 63/24/13% |
| Jun 15 | Iran vs New Zealand | Iran | 65/24/11% |
<!-- TRACKER:END -->

## Data

Match data from [martj42/international_results](https://github.com/martj42/international_results)
(includes scheduled 2026 fixtures, used as the prediction list in Phase 2).
