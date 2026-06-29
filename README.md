# World Cup Predictor

![CI](https://github.com/qnicondavid/World-Cup-Predictor/actions/workflows/ci.yml/badge.svg)

A prediction model for FIFA World Cup matches, trained on 150+ years of
international football results (49,000+ matches, 1872–present). Predictions are
served from a **Dixon-Coles goal model** with a squad market-value prior, locked
before kickoff, and scored against real results as the tournament unfolds. The
longer-term aim is the hard one: probabilities good enough to **beat the
bookmaker's closing line**, honestly verified.

**Live demo: [qnicondavid.github.io/World-Cup-Predictor](https://qnicondavid.github.io/World-Cup-Predictor/)**

## Live results

A GitHub Action runs daily: it pulls fresh results, locks predictions for
upcoming fixtures with the production model, scores completed ones, and rewrites
the tables below automatically.

<!-- TRACKER:START -->

**Record: 46/73 picks correct (63.0%), multiclass Brier 0.532, mean goal error 2.0** (uniform guess = 0.667)

| Date | Match | Pick | H/D/A | Pred (xG) | Result | Δ | Hit |
|---|---|---|---|---|---|---|---|
| Jun 28 | South Africa vs Canada | Canada | 19/30/50% | 0-1 (0.7–1.3) | 0-1 | 0 🎯 | ✅ |
| Jun 27 | Panama vs England | England | 8/19/73% | 0-2 (0.7–2.5) | 0-2 | 0 🎯 | ✅ |
| Jun 27 | Jordan vs Argentina | Argentina | 2/12/86% | 0-3 (0.5–3.7) | 1-3 | 1 | ✅ |
| Jun 27 | DR Congo vs Uzbekistan | Uzbekistan | 26/28/46% | 1-1 (1.1–1.5) | 3-1 | 2 | ❌ |
| Jun 27 | Croatia vs Ghana | Croatia | 81/15/4% | 3-0 (3.1–0.5) | 2-1 | 2 | ✅ |
| Jun 27 | Colombia vs Portugal | Colombia | 36/30/35% | 1-1 (1.3–1.3) | 0-0 | 2 | ❌ |
| Jun 27 | Algeria vs Austria | Austria | 33/30/37% | 1-1 (1.3–1.3) | 3-3 | 4 | ❌ |
| Jun 26 | Uruguay vs Spain | Spain | 10/21/69% | 0-2 (0.7–2.3) | 0-1 | 1 | ✅ |
| Jun 26 | Senegal vs Iraq | Senegal | 61/25/15% | 1-0 (2.0–0.8) | 5-0 | 4 | ✅ |
| Jun 26 | Norway vs France | France | 19/26/56% | 0-1 (0.9–1.8) | 1-4 | 4 | ✅ |
| Jun 26 | New Zealand vs Belgium | Belgium | 6/18/76% | 0-2 (0.6–2.8) | 1-5 | 4 | ✅ |
| Jun 26 | Egypt vs Iran | Iran | 22/27/51% | 1-1 (1.0–1.7) | 1-1 | 0 🎯 | ❌ |
| Jun 26 | Cape Verde vs Saudi Arabia | Saudi Arabia | 29/29/42% | 1-1 (1.2–1.5) | 0-0 | 2 | ❌ |
| Jun 25 | United States vs Turkey | Turkey | 27/28/45% | 1-1 (1.1–1.5) | 2-3 | 3 | ✅ |
| Jun 25 | Tunisia vs Netherlands | Netherlands | 8/19/73% | 0-2 (0.7–2.6) | 1-3 | 2 | ✅ |
| Jun 25 | Paraguay vs Australia | Paraguay | 37/30/33% | 1-1 (1.3–1.3) | 0-0 | 2 | ❌ |
| Jun 25 | Japan vs Sweden | Japan | 67/22/11% | 2-0 (2.3–0.7) | 1-1 | 2 | ❌ |
| Jun 25 | Ecuador vs Germany | Germany | 33/30/38% | 1-1 (1.2–1.4) | 2-1 | 1 | ❌ |
| Jun 25 | Curaçao vs Ivory Coast | Ivory Coast | 9/20/71% | 0-2 (0.7–2.5) | 0-2 | 0 🎯 | ✅ |
| Jun 24 | South Africa vs South Korea | South Korea | 9/20/70% | 0-2 (0.7–2.4) | 1-0 | 3 | ❌ |
| Jun 24 | Scotland vs Brazil | Brazil | 11/23/66% | 0-2 (0.8–2.2) | 0-3 | 1 | ✅ |
| Jun 24 | Morocco vs Haiti | Morocco | 78/17/5% | 2-0 (2.9–0.6) | 4-2 | 4 | ✅ |
| Jun 24 | Mexico vs Czech Republic | Mexico | 65/23/11% | 2-0 (2.2–0.8) | 3-0 | 1 | ✅ |
| Jun 24 | Canada vs Switzerland | Switzerland | 34/30/36% | 1-1 (1.3–1.3) | 1-2 | 1 | ✅ |
| Jun 24 | Bosnia and Herzegovina vs Qatar | Bosnia and Herzegovina | 52/27/21% | 1-0 (1.7–1.0) | 3-1 | 3 | ✅ |
| Jun 23 | Portugal vs Uzbekistan | Portugal | 68/22/10% | 2-0 (2.3–0.7) | 5-0 | 3 | ✅ |
| Jun 23 | Panama vs Croatia | Croatia | 15/25/60% | 0-1 (0.9–2.0) | 0-1 | 0 🎯 | ✅ |
| Jun 23 | England vs Ghana | England | 88/10/2% | 4-0 (4.0–0.4) | 0-0 | 4 | ❌ |
| Jun 23 | Colombia vs DR Congo | Colombia | 76/18/6% | 2-0 (2.8–0.6) | 1-0 | 1 | ✅ |
| Jun 22 | Norway vs Senegal | Norway | 48/28/24% | 1-1 (1.6–1.1) | 3-2 | 3 | ✅ |
| Jun 22 | Jordan vs Algeria | Algeria | 16/25/58% | 0-1 (0.9–1.9) | 1-2 | 2 | ✅ |
| Jun 22 | France vs Iraq | France | 84/13/3% | 3-0 (3.4–0.5) | 3-0 | 0 🎯 | ✅ |
| Jun 22 | Argentina vs Austria | Argentina | 71/20/9% | 2-0 (2.4–0.7) | 2-0 | 0 🎯 | ✅ |
| Jun 21 | Uruguay vs Cape Verde | Uruguay | 77/18/6% | 2-0 (2.8–0.6) | 2-2 | 2 | ❌ |
| Jun 21 | Spain vs Saudi Arabia | Spain | 91/8/2% | 4-0 (4.5–0.4) | 4-0 | 0 🎯 | ✅ |
| Jun 21 | New Zealand vs Egypt | Egypt | 22/27/52% | 0-1 (1.0–1.7) | 1-3 | 3 | ✅ |
| Jun 21 | Belgium vs Iran | Belgium | 49/27/23% | 1-1 (1.6–1.0) | 0-0 | 2 | ❌ |
| Jun 20 | Tunisia vs Japan | Japan | 8/19/73% | 0-2 (0.7–2.6) | 0-4 | 2 | ✅ |
| Jun 20 | Netherlands vs Sweden | Netherlands | 68/22/10% | 2-0 (2.3–0.7) | 5-1 | 4 | ✅ |
| Jun 20 | Germany vs Ivory Coast | Germany | 63/24/12% | 2-0 (2.1–0.8) | 2-1 | 1 | ✅ |
| Jun 20 | Ecuador vs Curaçao | Ecuador | 87/11/2% | 3-0 (3.9–0.4) | 0-0 | 3 | ❌ |
| Jun 19 | United States vs Australia | United States | 38/30/32% | 1-1 (1.4–1.2) | 2-0 | 2 | ✅ |
| Jun 19 | Turkey vs Paraguay | Turkey | 46/28/26% | 1-1 (1.5–1.1) | 0-1 | 1 | ❌ |
| Jun 19 | Scotland vs Morocco | Morocco | 17/25/58% | 0-1 (0.9–1.9) | 0-1 | 0 🎯 | ✅ |
| Jun 19 | Brazil vs Haiti | Brazil | 84/13/3% | 3-0 (3.4–0.5) | 3-0 | 0 🎯 | ✅ |
| Jun 18 | Switzerland vs Bosnia and Herzegovina | Switzerland | 76/18/6% | 2-0 (2.8–0.6) | 4-1 | 3 | ✅ |
| Jun 18 | Mexico vs South Korea | Mexico | 54/26/20% | 1-0 (1.8–1.0) | 1-0 | 0 🎯 | ✅ |
| Jun 18 | Czech Republic vs South Africa | Czech Republic | 59/25/16% | 1-0 (1.9–0.9) | 1-1 | 1 | ❌ |
| Jun 18 | Canada vs Qatar | Canada | 85/12/3% | 3-0 (3.6–0.5) | 6-0 | 3 | ✅ |
| Jun 17 | Uzbekistan vs Colombia | Colombia | 10/21/69% | 0-2 (0.7–2.3) | 1-3 | 2 | ✅ |
| Jun 17 | Portugal vs DR Congo | Portugal | 76/18/6% | 2-0 (2.8–0.6) | 1-1 | 2 | ❌ |
| Jun 17 | Ghana vs Panama | Panama | 13/25/62% | 0-2 (0.8–2.1) | 1-0 | 3 | ❌ |
| Jun 17 | England vs Croatia | England | 51/27/22% | 1-1 (1.7–1.0) | 4-2 | 4 | ✅ |
| Jun 16 | Iraq vs Norway | Norway | 9/20/71% | 0-2 (0.7–2.5) | 1-4 | 3 | ✅ |
| Jun 16 | France vs Senegal | France | 66/23/11% | 2-0 (2.2–0.8) | 3-1 | 2 | ✅ |
| Jun 16 | Austria vs Jordan | Austria | 60/25/15% | 1-0 (2.0–0.9) | 3-1 | 3 | ✅ |
| Jun 16 | Argentina vs Algeria | Argentina | 72/19/8% | 2-0 (2.5–0.7) | 3-0 | 1 | ✅ |
| Jun 15 | Spain vs Cape Verde | Spain | 93/5/2% | 5-0 (5.0–0.3) | 0-0 | 5 | ❌ |
| Jun 15 | Saudi Arabia vs Uruguay | Uruguay | 8/20/72% | 0-2 (0.7–2.5) | 1-1 | 2 | ❌ |
| Jun 15 | Iran vs New Zealand | Iran | 65/24/11% | 2-0 (2.2–0.8) | 2-2 | 2 | ❌ |
| Jun 15 | Belgium vs Egypt | Belgium | 63/24/13% | 2-0 (2.1–0.8) | 1-1 | 2 | ❌ |
| Jun 14 | Sweden vs Tunisia | Sweden | 42/29/29% | 1-1 (1.5–1.2) | 5-1 | 4 | ✅ |
| Jun 14 | Netherlands vs Japan | Netherlands | 36/30/35% | 1-1 (1.3–1.3) | 2-2 | 2 | ❌ |
| Jun 14 | Ivory Coast vs Ecuador | Ecuador | 14/25/61% | 0-2 (0.8–2.0) | 1-0 | 3 | ❌ |
| Jun 14 | Germany vs Curaçao | Germany | 88/10/2% | 4-0 (4.0–0.4) | 7-1 | 4 | ✅ |
| Jun 13 | Qatar vs Switzerland | Switzerland | 3/12/85% | 0-3 (0.5–3.6) | 1-1 | 3 | ❌ |
| Jun 13 | Haiti vs Scotland | Scotland | 15/25/60% | 0-1 (0.8–2.0) | 0-1 | 0 🎯 | ✅ |
| Jun 13 | Brazil vs Morocco | Brazil | 46/28/26% | 1-1 (1.5–1.1) | 1-1 | 0 🎯 | ❌ |
| Jun 13 | Australia vs Turkey | Turkey | 25/28/48% | 1-1 (1.1–1.6) | 2-0 | 2 | ❌ |
| Jun 12 | United States vs Paraguay | United States | 36/30/34% | 1-1 (1.3–1.3) | 4-1 | 3 | ✅ |
| Jun 12 | Canada vs Bosnia and Herzegovina | Canada | 75/18/6% | 2-0 (2.7–0.6) | 1-1 | 2 | ❌ |
| Jun 11 | South Korea vs Czech Republic | South Korea | 40/28/32% | 1-1 (1.3–1.1) | 2-1 | 1 | ✅ |
| Jun 11 | Mexico vs South Africa | Mexico | 63/24/13% | 1-0 (1.8–0.7) | 2-0 | 1 | ✅ |

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
| Jul 2 | Portugal vs Croatia | Portugal | 50/27/23% | 1-0 (1.5–0.9) |
| Jul 2 | Switzerland vs Algeria | Switzerland | 40/28/32% | 1-1 (1.4–1.2) |
| Jul 3 | Australia vs Egypt | Australia | 36/35/30% | 0-0 (0.9–0.8) |
| Jul 3 | Argentina vs Cape Verde | Argentina | 77/18/5% | 2-0 (2.1–0.4) |
| Jul 3 | Colombia vs Ghana | Colombia | 64/24/12% | 1-0 (1.8–0.6) |

<!-- TRACKER:END -->

### Championship odds

<!-- TITLE:START -->
The model's championship odds from 10,000 Monte Carlo simulations, updated 2026-06-29. They inherit the simulator's simplifications (knockout bracket paired in schedule order, games as neutral with no draws), so read them as the model's view, not a hard forecast.

| # | Team | Title | Final | Semis |
|---|---|---|---|---|
| 1 | Argentina | 25.1% | 38.1% | 56.9% |
| 2 | France | 19.4% | 34.0% | 48.5% |
| 3 | Spain | 15.7% | 25.9% | 45.5% |
| 4 | England | 6.4% | 14.3% | 24.5% |
| 5 | Brazil | 6.0% | 13.3% | 28.1% |
| 6 | Colombia | 5.0% | 10.8% | 21.4% |
| 7 | Netherlands | 3.1% | 7.8% | 17.8% |
| 8 | Portugal | 3.0% | 6.7% | 16.1% |
| 9 | Germany | 2.4% | 6.3% | 16.9% |
| 10 | Morocco | 2.3% | 5.7% | 14.0% |
| 11 | Belgium | 1.7% | 4.9% | 14.4% |
| 12 | Japan | 1.7% | 4.7% | 12.2% |
| 13 | Switzerland | 1.4% | 4.0% | 10.5% |
| 14 | Mexico | 1.3% | 4.0% | 8.9% |
| 15 | Norway | 1.3% | 3.8% | 8.4% |
| 16 | Ecuador | 0.9% | 2.6% | 6.1% |

<!-- TITLE:END -->

## Track record on past World Cups

Before predicting 2026, the model is validated on five World Cups it never saw
during training. For each tournament it trains only on matches played before it,
then predicts every match in it, the same information regime as predicting live.

| Tournament | Tuned model | Baseline |
|---|---|---|
| World Cup 2022 | 32/64 (50.0%), Brier 0.183 | 34/64 (53.1%), Brier 0.181 |
| World Cup 2018 | 37/64 (57.8%), Brier 0.159 | 34/64 (53.1%), Brier 0.167 |
| World Cup 2014 | 39/64 (60.9%), Brier 0.135 | 39/64 (60.9%), Brier 0.150 |
| World Cup 2010 | 35/64 (54.7%), Brier 0.146 | 32/64 (50.0%), Brier 0.148 |
| World Cup 2006 | 41/64 (64.1%), Brier 0.119 | 42/64 (65.6%), Brier 0.129 |
| **Combined (320)** | **184/320 (57.5%), Brier 0.148** | 181/320 (56.6%), Brier 0.155 |
| Coin-flip reference | 50%, Brier 0.250 | n/a |

The tuned model beats both the baseline and a coin flip across the combined 320
matches. One pattern stands out: **World Cups are getting harder to predict.**
Brier rises almost monotonically from 0.119 (2006) to 0.183 (2022); the field
has genuinely tightened.

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
data/results.csv -> MatchCsvParser -> Dixon-Coles fit (+ value prior) -> scoreline distribution -> predictions
```

The original engine is an **Elo rating system**: every team starts at 1500, and
after each match ratings shift by `K * (actual - expected)` with K scaled by
match importance and a home boost at non-neutral venues. Elo still drives the
Monte Carlo title simulation and remains the baseline every goal model is
measured against.

## Why you can trust it

**Held out, every time.** Every figure in the track record above comes from
World Cups the model never trained on, predicting each match before its result,
so nothing is fit to the games it is judged on. The live 2026 picks follow the
same discipline.

**Locked, never edited.** Live predictions are written to
`predictions/predictions.csv` before kickoff and never changed; the git history
is the audit trail. A prediction made under an older model is preserved as-is
and never re-locked.

**Negative findings are kept, not hidden.** Several intuitive ideas were tested
honestly and did not earn their place, and they stay documented because that is
why the rest can be believed:

- **Rest-days advantage**: no out-of-sample improvement (`--rest`).
- **Bivariate covariance term**: adds nothing over Dixon-Coles (Brier 0.573 vs
  0.574).
- **Temperature scaling**: sharpened in-sample but made held-out 2022 worse on
  every metric, so none is applied.
- **Annual regression toward the mean**: never helped at any strength;
  national-team strength is more persistent than folklore suggests.

What did survive: goal-margin scaling (combined Brier 0.148 vs 0.155) and a
small, tuned squad-value prior.

## Methodology in depth

### Draw modelling

The Elo expected score conflates winning and drawing (E = P(win) + P(draw)/2).
To split it, P(draw) is estimated empirically: replaying 37,314 internationals
since 1980 through the model shows the draw rate falling from ~30% between equal
teams to ~2% at a 600-point rating gap. `DrawModel` interpolates that observed
curve and splits E into explicit win/draw/loss probabilities. An honest
limitation: the model never makes "draw" its single most likely outcome (~30% is
the ceiling), so the draw model improves probabilities, not picks;
bookmakers share this property.

### Goal models

A goal model gives every team a separate attack and defence rating and predicts
the scoreline, yielding win/draw/loss probabilities from first principles
rather than from an empirical draw curve. Three live under
`com.david.worldcup.goals`, comparable head-to-head via `--goals`:

- **Dixon-Coles**: the production model (described above).
- **Bivariate Poisson**: the same attack/defence fit plus a shared component
  that makes the two scores positively correlated.
- **Elo-Poisson**: the lightweight option, reusing the Elo gap and mapping it to
  two Poisson rates by regression.

Scored on 320 World Cup matches (2006-2022), train-before-each-tournament:

| Model | Picks correct | Combined multiclass Brier |
|---|---|---|
| Dixon-Coles | 183/320 (57.2%) | 0.574 |
| Bivariate Poisson | 183/320 (57.2%) | 0.573 |
| Elo-Poisson | 178/320 (55.6%) | 0.575 |
| Elo + DrawModel (baseline) | 178/320 (55.6%) | 0.576 |
| Uniform reference | n/a | 0.667 |

The goal models edge the Elo baseline modestly. The edge is uneven across
tournaments, so an **Elo + Dixon-Coles ensemble** (averaging the two probability
vectors) is also wired into `--goals`. The fitter itself is validated against
`research/goal_models.py`: on data from a known Dixon-Coles process it recovers
team attack strengths at correlation 0.99.

### Squad market value

Elo and goal ratings are lagging: they learn strength from results. Squad
market value is a leading signal. The model folds it in as a **prior on the
Dixon-Coles attack/defence ratings**: a richer-than-average squad gets a higher
attack and lower (better) defence prior, and each team's fitted rating is shrunk
toward that prior. It reads `data/market_values.csv` (`team,as_of,value_eur`);
lookups always take the most recent value on or before the match date, so
nothing leaks from the future.

`--values-tune` grid-searches the weights on 2006-2018 and validates once on
held-out 2022. The tuned prior (`globalWeight 0.20, sparseWeight 0, valueScale
0.30`) beats plain Dixon-Coles out of sample (multiclass Brier **0.6065 vs
0.6123** on 2022), so it is now the default. Caveats kept in view: the gain is
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

A well-calibrated model only has value if it disagrees with the market.
`--bets` closes that loop: it reads bookmaker odds from `data/odds_sample.csv`,
strips the overround to fair probabilities, and for each upcoming fixture
compares the model's win/draw/loss probabilities to the price. When an outcome's
expected value clears a threshold it is flagged and sized by fractional Kelly.
The default policy is deliberately conservative (a 5% edge floor, quarter-Kelly,
capped at 5% of bankroll) because calibration wobbles between tournaments.

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

- [dcaribou/transfermarkt-datasets](https://github.com/dcaribou/transfermarkt-datasets):
  the best fit, a `player_valuations` table with dated market values plus
  national-team data, refreshed weekly. Aggregate player valuations to a squad
  total per national team per date to build `market_values.csv`.
- [salimt/football-datasets](https://github.com/salimt/football-datasets) and the
  Kaggle mirror [davidcariboo/player-scores](https://www.kaggle.com/datasets/davidcariboo/player-scores)
  are alternatives.
