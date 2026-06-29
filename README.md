# World Cup Elo Predictor

![CI](https://github.com/qnicondavid/World-Cup-Predictor/actions/workflows/ci.yml/badge.svg)

A prediction model for FIFA World Cup matches, trained on 150+ years of
international football results (49,000+ matches, 1872–present). Predictions are
served from a **Dixon-Coles goal model** with a squad market-value prior, locked
before kickoff, and scored against real results as the tournament unfolds. The
longer-term aim is the hard one: probabilities good enough to **beat the
bookmaker's closing line**, honestly verified.

**▶ Live demo: [qnicondavid.github.io/World-Cup-Predictor](https://qnicondavid.github.io/World-Cup-Predictor/)** —
daily-updated 2026 predictions, scored record, and Monte Carlo championship odds.

## Live results

A GitHub Action runs daily: it pulls fresh results, locks predictions for
upcoming fixtures with the production model, scores completed ones, and rewrites
the tables below automatically. **Predictions are locked before kickoff and
never edited** — the git history of `predictions/predictions.csv` is the proof.

<!-- TRACKER:START -->
_Updated 2026-06-29 — predictions are locked before kickoff and never edited; the git history of `predictions/predictions.csv` is the proof. Each pick is the model's most likely outcome and the H/D/A column its full home-win / draw / away-win split; the predicted score is the most likely scoreline (expected goals in brackets), and Δ is the total goal difference from the actual result (🎯 = exact). Brier is multiclass._

**Record: 44/71 picks correct (62.0%) — multiclass Brier 0.536 — mean goal error 2.0** (uniform guess = 0.667)

| Date | Match | Pick | H/D/A | Pred (xG) | Result | Δ | Hit |
|---|---|---|---|---|---|---|---|
| Jun 25 | Japan vs Sweden | Japan | 67/22/11% | 2-0 (2.3–0.7) | 1-1 | 2 | ❌ |
| Jun 25 | Tunisia vs Netherlands | Netherlands | 8/19/73% | 0-2 (0.7–2.6) | 1-3 | 2 | ✅ |
| Jun 26 | Egypt vs Iran | Iran | 22/27/51% | 1-1 (1.0–1.7) | 1-1 | 0 🎯 | ❌ |
| Jun 26 | New Zealand vs Belgium | Belgium | 6/18/76% | 0-2 (0.6–2.8) | 1-5 | 4 | ✅ |
| Jun 26 | Cape Verde vs Saudi Arabia | Saudi Arabia | 29/29/42% | 1-1 (1.2–1.5) | 0-0 | 2 | ❌ |
| Jun 26 | Uruguay vs Spain | Spain | 10/21/69% | 0-2 (0.7–2.3) | 0-1 | 1 | ✅ |
| Jun 26 | Norway vs France | France | 19/26/56% | 0-1 (0.9–1.8) | 1-4 | 4 | ✅ |
| Jun 26 | Senegal vs Iraq | Senegal | 61/25/15% | 1-0 (2.0–0.8) | 5-0 | 4 | ✅ |
| Jun 27 | Algeria vs Austria | Austria | 33/30/37% | 1-1 (1.3–1.3) | 3-3 | 4 | ❌ |
| Jun 27 | Jordan vs Argentina | Argentina | 2/12/86% | 0-3 (0.5–3.7) | 1-3 | 1 | ✅ |
| Jun 27 | Colombia vs Portugal | Colombia | 36/30/35% | 1-1 (1.3–1.3) | 0-0 | 2 | ❌ |
| Jun 27 | DR Congo vs Uzbekistan | Uzbekistan | 26/28/46% | 1-1 (1.1–1.5) | 3-1 | 2 | ❌ |
| Jun 27 | Panama vs England | England | 8/19/73% | 0-2 (0.7–2.5) | 0-2 | 0 🎯 | ✅ |
| Jun 27 | Croatia vs Ghana | Croatia | 81/15/4% | 3-0 (3.1–0.5) | 2-1 | 2 | ✅ |
| Jun 28 | South Africa vs Canada | Canada | 19/30/50% | 0-1 (0.7–1.3) | 0-1 | 0 🎯 | ✅ |

**Locked for upcoming matches:**

| Date | Match | Pick | H/D/A | Pred (xG) |
|---|---|---|---|---|
| Jun 29 | Brazil vs Japan | Brazil | 45/29/26% | 1-0 (1.4–1.0) |
| Jun 29 | Germany vs Paraguay | Germany | 53/27/21% | 1-0 (1.6–0.9) |
| Jun 29 | Netherlands vs Morocco | Morocco | 33/32/35% | 1-1 (1.0–1.0) |
| Jun 30 | Ivory Coast vs Norway | Norway | 30/30/40% | 1-1 (1.0–1.2) |
| Jun 30 | France vs Sweden | France | 62/22/16% | 2-0 (2.0–1.0) |
| Jun 30 | Mexico vs Ecuador | Mexico | 35/34/31% | 0-0 (0.9–0.9) |
| Jul 1 | England vs DR Congo | England | 57/30/14% | 1-0 (1.3–0.5) |
| Jul 1 | Belgium vs Senegal | Belgium | 43/29/28% | 1-1 (1.4–1.0) |
| Jul 1 | United States vs Bosnia and Herzegovina | United States | 64/21/15% | 2-0 (2.1–0.9) |
| Jul 2 | Spain vs Austria | Spain | 58/25/17% | 1-0 (1.7–0.8) |

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

### Championship odds

The model's title picks from 10,000 Monte Carlo simulations of the rest of the
tournament, refreshed daily by the same Action.

<!-- TITLE:START -->
_The model's championship odds from 10,000 Monte Carlo simulations, updated 2026-06-29. They inherit the simulator's simplifications (Elo tie-breaks, seeded knockout pairings, knockout games as neutral with no draws), so read them as the model's view, not a hard forecast._

| # | Team | Title | Final | Semis |
|---|---|---|---|---|
| 1 | Argentina | 40.6% | 67.2% | 96.1% |
| 2 | France | 29.3% | 56.9% | 89.3% |
| 3 | England | 14.2% | 35.0% | 91.6% |
| 4 | Colombia | 12.5% | 31.2% | 95.0% |
| 5 | Mexico | 1.5% | 4.6% | 11.6% |
| 6 | Spain | 0.8% | 1.4% | 2.2% |
| 7 | Croatia | 0.5% | 1.8% | 8.5% |
| 8 | Portugal | 0.5% | 1.5% | 4.9% |
| 9 | Brazil | 0.1% | 0.3% | 0.4% |
| 10 | Netherlands | 0.0% | 0.1% | 0.2% |
| 11 | Switzerland | 0.0% | 0.0% | 0.1% |
| 12 | Algeria | 0.0% | 0.0% | 0.0% |
| 13 | Australia | 0.0% | 0.0% | 0.0% |
| 14 | Austria | 0.0% | 0.0% | 0.0% |
| 15 | Belgium | 0.0% | 0.0% | 0.0% |
| 16 | Bosnia and Herzegovina | 0.0% | 0.0% | 0.0% |

<!-- TITLE:END -->

## How it works

The production model is **Dixon-Coles**: every team gets a separate **attack**
and **defence** rating, fit by weighted maximum likelihood on all of
international history, with a home-advantage term, the low-score correlation
correction (`rho`) that fixes Poisson's under-counting of 0-0 and 1-1,
exponential time decay (2-year half-life), and shrinkage toward the average for
rarely-seen teams. Squad market value is folded in as a **prior** on those
ratings. From the fitted attack/defence pair the model produces a full scoreline
distribution, and from that the win/draw/loss probabilities and most-likely
score you see above.

```
data/results.csv → MatchCsvParser → Dixon-Coles fit (+ value prior) → scoreline distribution → predictions
```

The original engine is an **Elo rating system** — every team starts at 1500, and
after each match ratings shift by `K * (actual − expected)` with K scaled by
match importance and a home boost at non-neutral venues. Elo still drives the
Monte Carlo title simulation and remains the baseline every goal model is
measured against.

## Why you can trust it

**Held out, every time.** The model is scored on five World Cups it has never
seen: for each tournament it trains only on matches played before it, then
predicts each match *before* learning its result — the same information regime
as predicting in real time.

| Tournament | Tuned model | Baseline |
|---|---|---|
| World Cup 2006 | 41/64 (64.1%), Brier 0.119 | 42/64 (65.6%), Brier 0.129 |
| World Cup 2010 | 35/64 (54.7%), Brier 0.146 | 32/64 (50.0%), Brier 0.148 |
| World Cup 2014 | 39/64 (60.9%), Brier 0.135 | 39/64 (60.9%), Brier 0.150 |
| World Cup 2018 | 37/64 (57.8%), Brier 0.159 | 34/64 (53.1%), Brier 0.167 |
| World Cup 2022 | 32/64 (50.0%), Brier 0.183 | 34/64 (53.1%), Brier 0.181 |
| **Combined (320)** | **184/320 (57.5%), Brier 0.148** | 181/320 (56.6%), Brier 0.155 |
| Coin-flip reference | 50%, Brier 0.250 | — |

**Locked, never edited.** Live predictions are written to
`predictions/predictions.csv` before kickoff and never changed; the git history
is the audit trail. A prediction made under an older model is preserved as-is —
it is never re-locked.

**Negative findings are kept, not hidden.** Several intuitive ideas were tested
honestly and did not earn their place, and they stay documented because *that is
why the rest can be believed*:

- **Rest-days advantage** — no out-of-sample improvement (`--rest`).
- **Bivariate covariance term** — adds nothing over Dixon-Coles (Brier 0.573 vs
  0.574).
- **Temperature scaling** — sharpened in-sample but made held-out 2022 worse on
  every metric, so none is applied.
- **Annual regression toward the mean** — never helped at any strength;
  national-team strength is more persistent than folklore suggests.

What *did* survive: goal-margin scaling (combined Brier 0.148 vs 0.155) and a
small, tuned squad-value prior. A note worth keeping in view — **World Cups are
getting harder to predict**: Brier rises almost monotonically from 0.119 (2006)
to 0.183 (2022). The field has genuinely tightened.

## Methodology in depth

### Draw modelling

The Elo expected score conflates winning and drawing (E = P(win) + P(draw)/2).
To split it, P(draw) is estimated empirically: replaying 37,314 internationals
since 1980 through the model shows the draw rate falling from ~30% between equal
teams to ~2% at a 600-point rating gap. `DrawModel` interpolates that observed
curve and splits E into explicit win/draw/loss probabilities. An honest
limitation: the model never makes "draw" its single most likely outcome (~30% is
the ceiling), so the draw model improves *probabilities*, not *picks* —
bookmakers share this property.

### Goal models

A goal model gives every team a separate attack and defence rating and predicts
the *scoreline*, yielding win/draw/loss probabilities from first principles
rather than from an empirical draw curve. Three live under
`com.david.worldcup.goals`, comparable head-to-head via `--goals`:

- **Dixon-Coles** — the production model (described above).
- **Bivariate Poisson** — the same attack/defence fit plus a shared component
  that makes the two scores positively correlated.
- **Elo-Poisson** — the lightweight option: reuse the Elo gap and map it to two
  Poisson rates by regression.

Scored on 320 World Cup matches (2006-2022), train-before-each-tournament:

| Model | Picks correct | Combined multiclass Brier |
|---|---|---|
| Dixon-Coles | 183/320 (57.2%) | 0.574 |
| Bivariate Poisson | 183/320 (57.2%) | 0.573 |
| Elo-Poisson | 178/320 (55.6%) | 0.575 |
| Elo + DrawModel (baseline) | 178/320 (55.6%) | 0.576 |
| Uniform reference | — | 0.667 |

The goal models edge the Elo baseline modestly. The edge is uneven across
tournaments, so an **Elo + Dixon-Coles ensemble** (averaging the two probability
vectors) is also wired into `--goals`. The fitter itself is validated against
`research/goal_models.py`: on data from a known Dixon-Coles process it recovers
team attack strengths at correlation 0.99.

### Squad market value

Elo and goal ratings are *lagging* — they learn strength from results. Squad
market value is a *leading* signal. The model folds it in as a **prior on the
Dixon-Coles attack/defence ratings**: a richer-than-average squad gets a higher
attack and lower (better) defence prior, and each team's fitted rating is shrunk
toward that prior. It reads `data/market_values.csv` (`team,as_of,value_eur`);
lookups always take the most recent value *on or before* the match date, so
nothing leaks from the future.

`--values-tune` grid-searches the weights on 2006-2018 and validates once on
held-out 2022. The tuned prior (`globalWeight 0.20, sparseWeight 0, valueScale
0.30`) beats plain Dixon-Coles out of sample — multiclass Brier **0.6065 vs
0.6123** on 2022 — so it is now the default. Caveats kept in view: the gain is
small (~0.006 Brier, identical pick accuracy), it rests on a single held-out
tournament, and the sparse-team lever earned nothing. The shipped
`market_values.csv` is a small **illustrative** sample; replace it with real
data (see Data below).

### Calibration

`--calibrate` audits the production model on the held-out World Cups: reliability
bins, log-loss, multiclass Brier and expected calibration error (ECE), plus a
temperature fit. The finding: the model is mildly **under-confident** (ECE ≈
0.06), but the calibration direction is not stable across tournaments, so no
temperature is applied and the raw probabilities ship as-is. The practical
consequence for any betting layer is to demand a margin of safety and size
conservatively.

### Value betting (toward beating the book)

A well-calibrated model only has *value* if it disagrees with the market.
`--bets` closes that loop: it reads bookmaker odds from `data/odds_sample.csv`,
strips the overround to fair probabilities, and for each upcoming fixture
compares the model's win/draw/loss probabilities to the price. When an outcome's
expected value clears a threshold it is flagged and sized by fractional Kelly.
The default policy is deliberately conservative — a 5% edge floor, quarter-Kelly,
capped at 5% of bankroll — because calibration wobbles between tournaments.

The shipped odds file is **mock** data. Historical international closing odds
barely exist, so the credible path is forward-testing: wire a live feed (e.g.
The Odds API free tier), lock each flagged bet at its pre-kickoff price, and
track ROI forward with the same never-edited discipline as the predictions.

### Rest-days differential (experimental)

`--rest` tests whether a team with more recovery than its opponent has an edge
the rating misses: it adds rating points per day of rest advantage (capped at 10
days) and measures held-out Brier against the unadjusted baseline. It needs no
new data, so it is fully reproducible. As noted above, the effect did not survive
out of sample.

## Run it

Requires JDK 17+ and Maven.

```bash
mvn test                            # run the unit test suite
mvn compile exec:java               # replay history, print Elo top 15 + sample predictions
mvn compile exec:java -Dexec.args="--backtest"   # evaluate on the held-out World Cups
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

## Data & credits

Match data from [martj42/international_results](https://github.com/martj42/international_results)
(includes scheduled 2026 fixtures, used as the prediction list). Refreshed daily
by the tracker Action.

Squad market values come from the Transfermarkt community datasets (no public
API exists, so download where you have network access, not from CI):

- [dcaribou/transfermarkt-datasets](https://github.com/dcaribou/transfermarkt-datasets)
  — the best fit: a `player_valuations` table with *dated* market values plus
  national-team data, refreshed weekly. Aggregate player valuations to a squad
  total per national team per date to build `market_values.csv`.
- [salimt/football-datasets](https://github.com/salimt/football-datasets) and the
  Kaggle mirror [davidcariboo/player-scores](https://www.kaggle.com/datasets/davidcariboo/player-scores)
  are alternatives.
