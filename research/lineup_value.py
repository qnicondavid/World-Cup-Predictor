#!/usr/bin/env python3
"""
research/lineup_value.py - Phase 2 of the lineup-weighted value feature.

Reads data/lineups.csv (starting XIs from research/fetch_lineups.py) plus the
Transfermarkt panel in data/transfermarkt/, matches each starter to a player_id
(exact -> surname/token -> fuzzy, restricted to nationality), values them as of
the match date, imputes missing starters with the XI median, and writes
data/lineup_values.csv:

    match_date,home_team,away_team,home_xi_value,away_xi_value,home_valued,away_valued

home_valued / away_valued are how many of the 11 starters had a real valuation
(the rest were imputed) - Phase 3 uses this to shrink thin sides toward the
squad-value baseline.
"""
import bisect
import collections
import csv
import gzip
import statistics as st
import unicodedata
import os

_HERE = os.path.dirname(os.path.abspath(__file__))
_DATA = os.path.join(_HERE, "..", "data")


def norm(s):
    s = unicodedata.normalize("NFKD", s).encode("ascii", "ignore").decode().lower()
    return " ".join("".join(c if c.isalnum() or c == " " else " " for c in s).split())


def load_players():
    by_country = collections.defaultdict(list)
    countries = set()
    with gzip.open(os.path.join(_DATA, "transfermarkt", "players.csv.gz"), "rt", encoding="utf-8") as f:
        for r in csv.DictReader(f):
            c = r["country_of_citizenship"]
            countries.add(c)
            nm = norm(r["name"])
            if nm:
                by_country[c].append((nm, r["player_id"], frozenset(nm.split())))
    return by_country, countries


def load_valuations():
    val = collections.defaultdict(list)
    with gzip.open(os.path.join(_DATA, "transfermarkt", "player_valuations.csv.gz"), "rt", encoding="utf-8") as f:
        for r in csv.DictReader(f):
            val[r["player_id"]].append((r["date"], float(r["market_value_in_eur"])))
    for p in val:
        val[p].sort()
    return val


def value_asof(val, pid, date):
    rows = val.get(pid)
    if not rows:
        return None
    i = bisect.bisect_right([d for d, _ in rows], date)
    return rows[i - 1][1] if i > 0 else None


def best_match(name_norm, country, by_country):
    import difflib
    cands = by_country.get(country, [])
    if not cands:
        return None
    for nm, pid, _ in cands:
        if nm == name_norm:
            return pid
    qt = set(name_norm.split())
    last = name_norm.split()[-1]
    best, br = None, 0.0
    for nm, pid, tk in cands:
        share = len(qt & tk)
        if last in tk or share >= 2:
            rr = difflib.SequenceMatcher(None, name_norm, nm).ratio() + 0.05 * share
            if rr > br:
                br, best = rr, pid
    if best and br >= 0.55:
        return best
    for nm, pid, _ in cands:
        rr = difflib.SequenceMatcher(None, name_norm, nm).ratio()
        if rr > br:
            br, best = rr, pid
    return best if br >= 0.86 else None


def country_aliases(countries):
    alias = {"Ivory Coast": "Cote d'Ivoire", "IR Iran": "Iran"}
    for k in countries:
        if k.lower() in ("korea, south", "south korea"):
            alias["South Korea"] = k
        if k.lower() in ("korea, north", "north korea"):
            alias["North Korea"] = k
    return alias


def main():
    by_country, countries = load_players()
    val = load_valuations()
    alias = country_aliases(countries)

    xi = [r for r in csv.DictReader(open(os.path.join(_DATA, "lineups.csv"), encoding="utf-8"))
          if r["started"] == "1"]
    per_match = collections.defaultdict(lambda: collections.defaultdict(list))
    matched = valued = total = 0
    for r in xi:
        total += 1
        c = alias.get(r["team"], r["team"])
        pid = best_match(norm(r["player_name"]), c, by_country)
        v = value_asof(val, pid, r["match_date"]) if pid else None
        matched += pid is not None
        valued += v is not None
        per_match[(r["match_date"], r["home_team"], r["away_team"])][r["team"]].append(v)
    print(f"player match rate {100*matched/total:.0f}%  valued-as-of {100*valued/total:.0f}%  of {total} starters")

    rows = []
    for (d, h, a), teams in per_match.items():
        tv = {}
        for team, vals in teams.items():
            present = [v for v in vals if v is not None]
            med = st.median(present) if present else None
            filled = sum((v if v is not None else med) for v in vals) if med is not None else None
            tv[team] = (filled, len(present))
        hv, av = tv.get(h, (None, 0)), tv.get(a, (None, 0))
        rows.append((d, h, a, hv[0], av[0], hv[1], av[1]))

    out = os.path.join(_DATA, "lineup_values.csv")
    with open(out, "w", newline="", encoding="utf-8") as f:
        w = csv.writer(f)
        w.writerow(["match_date", "home_team", "away_team",
                    "home_xi_value", "away_xi_value", "home_valued", "away_valued"])
        w.writerows(rows)
    print(f"wrote {len(rows)} matches to {out}")


if __name__ == "__main__":
    main()
