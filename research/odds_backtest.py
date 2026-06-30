#!/usr/bin/env python3
"""
research/odds_backtest.py - backtest the model against historical bookmaker odds.

Supply data/odds_history.csv with (at least) these columns; extra columns are
ignored and column names are matched case-insensitively:

    match_date,home_team,away_team,home_odds,draw_odds,away_odds

odds are decimal (e.g. 1.80). Team names should match data/results.csv; a small
alias map below handles the common international naming differences. Get the odds
from OddsPortal / Betfair historical / a WC-covering Kaggle set; the 20k "Europe"
club datasets will NOT have these matches.

The script joins to research/export_predictions_form.csv (the production model's
held-out predictions) for WC2018 + WC2022, de-vigs the odds to market
probabilities, and reports:
  * coverage (matches joined)
  * model multiclass Brier vs the market (de-vigged closing-line) Brier on the
    SAME matches - the board's actual question: are we sharper than the price?
  * value bets the policy would flag (5% edge floor, quarter-Kelly) and their ROI
  * mean closing-line value (model prob minus market prob on the bet side)
"""
import csv
import math
import os
import unicodedata

_HERE = os.path.dirname(os.path.abspath(__file__))
_DATA = os.path.join(_HERE, "..", "data")

# odds-source team name -> data/results.csv name (extend as the report flags gaps)
ALIASES = {
    "korea republic": "South Korea", "south korea": "South Korea",
    "ir iran": "Iran", "iran": "Iran",
    "usa": "United States", "united states of america": "United States",
    "cote d'ivoire": "Ivory Coast", "ivory coast": "Ivory Coast",
    "czechia": "Czech Republic", "turkiye": "Turkey",
}


def canon(team):
    t = unicodedata.normalize("NFKD", team).encode("ascii", "ignore").decode().lower().strip()
    return ALIASES.get(t, team.strip())


def devig(o_home, o_draw, o_away):
    inv = [1.0 / o_home, 1.0 / o_draw, 1.0 / o_away]
    s = sum(inv)
    return [x / s for x in inv]


def mcb(p, y):
    e = [0, 0, 0]
    e[y] = 1
    return sum((p[i] - e[i]) ** 2 for i in range(3))


def load_odds():
    path = os.path.join(_DATA, "odds_history.csv")
    if not os.path.exists(path):
        raise SystemExit("Provide data/odds_history.csv first (see the module docstring).")
    rows = {}
    with open(path, encoding="utf-8") as f:
        r = csv.DictReader(f)
        cols = {c.lower(): c for c in r.fieldnames}
        need = ["match_date", "home_team", "away_team", "home_odds", "draw_odds", "away_odds"]
        for n in need:
            if n not in cols:
                raise SystemExit(f"odds_history.csv missing column '{n}'. Have: {r.fieldnames}")
        for row in r:
            d = row[cols["match_date"]][:10]
            h = canon(row[cols["home_team"]])
            a = canon(row[cols["away_team"]])
            try:
                oh, od, oa = (float(row[cols["home_odds"]]), float(row[cols["draw_odds"]]),
                              float(row[cols["away_odds"]]))
            except ValueError:
                continue
            rows[(d, h, a)] = (oh, od, oa)
    return rows


def main():
    odds = load_odds()
    EDGE_FLOOR, KELLY = 0.05, 0.25
    joined = []
    with open(os.path.join(_DATA, "..", "research", "export_predictions_form.csv"), encoding="utf-8") as f:
        for r in csv.DictReader(f):
            if r["tournament"] not in ("WC2018", "WC2022"):
                continue
            key = (r["date"][:10], canon(r["home"]), canon(r["away"]))
            o = odds.get(key)
            if not o:
                continue
            y = {"home": 0, "draw": 1, "away": 2}[r["actual"]]
            model = [float(r["p_home"]), float(r["p_draw"]), float(r["p_away"])]
            market = devig(*o)
            joined.append((r["tournament"], model, market, list(o), y))

    n = len(joined)
    print(f"joined {n} of 128 WC2018+WC2022 matches with odds")
    if n == 0:
        raise SystemExit("No matches joined - check team-name aliases and date formats.")

    mb = sum(mcb(m, y) for _, m, _, _, y in joined) / n
    kb = sum(mcb(k, y) for _, _, k, _, y in joined) / n
    print(f"model Brier {mb:.4f}   market (de-vigged) Brier {kb:.4f}   diff {mb-kb:+.4f}")
    print("  (model sharper than the price only if diff is clearly negative)")

    # value bets: stake quarter-Kelly when model EV over an outcome clears the floor
    bankroll_units, staked, pnl, clv = 0.0, 0.0, 0.0, []
    bets = 0
    for _, model, market, o, y in joined:
        for i in range(3):
            ev = model[i] * o[i] - 1.0
            if ev <= EDGE_FLOOR:
                continue
            kelly = max(0.0, (model[i] * o[i] - 1.0) / (o[i] - 1.0)) * KELLY
            stake = min(kelly, 0.05)
            bets += 1
            staked += stake
            pnl += stake * (o[i] - 1.0) if y == i else -stake
            clv.append(model[i] - market[i])
    roi = (pnl / staked * 100) if staked else 0.0
    print(f"value bets flagged: {bets}   ROI {roi:+.1f}% of staked   "
          f"mean CLV {sum(clv)/len(clv):+.4f}" if clv else f"value bets flagged: {bets}")


if __name__ == "__main__":
    main()
