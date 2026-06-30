#!/usr/bin/env python3
"""
research/fetch_lineups.py - collect World Cup starting XIs into data/lineups.csv.

Run this on a machine with internet access (not the project sandbox):

    python3 research/fetch_lineups.py

Source: StatsBomb open data (https://github.com/statsbomb/open-data), which
covers the men's FIFA World Cup 2018 and 2022 with full lineups. Each player's
"positions" carry a start_reason, so we can tell starters from substitutes
without downloading the (much larger) event files.

2006 / 2010 / 2014 are NOT in StatsBomb open data and have no clean free
structured lineup feed; see the note at the bottom of this file. The validation
plan deliberately starts on 2018 + 2022 (best valuation coverage, hardest
tournaments) to prove the signal before any harder backfill.

Output schema (one row per player per match):
    match_date,home_team,away_team,team,player_name,started   (started in {0,1})
"""
import csv
import json
import os
import sys
import urllib.request

BASE = "https://raw.githubusercontent.com/statsbomb/open-data/master/data"

# label -> (competition_id, season_id) in StatsBomb open data
WORLD_CUPS = {
    "2018": (43, 3),
    "2022": (43, 106),
}

# StatsBomb team name -> the name used in data/results.csv.
# The script reports any team it could not match so you can extend this map.
TEAM_ALIASES = {
    "IR Iran": "Iran",
    "Korea Republic": "South Korea",
    "China PR": "China",
}

_HERE = os.path.dirname(os.path.abspath(__file__))
_DATA = os.path.join(_HERE, "..", "data")


def fetch(url):
    with urllib.request.urlopen(url, timeout=60) as r:
        return json.load(r)


def normalize(team):
    return TEAM_ALIASES.get(team, team)


def is_starter(player):
    """A player started if a position begins as 'Starting XI' (or, in older
    schemas without start_reason, begins at 00:00 of the first period)."""
    for p in player.get("positions", []):
        reason = p.get("start_reason")
        if reason == "Starting XI":
            return True
        if reason is None and p.get("from") in ("00:00", "0:00") and p.get("from_period") == 1:
            return True
    return False


def known_team_names():
    names = set()
    path = os.path.join(_DATA, "results.csv")
    if os.path.exists(path):
        for r in csv.DictReader(open(path, encoding="utf-8")):
            names.add(r["home_team"])
            names.add(r["away_team"])
    return names


def collect():
    rows = []
    unmatched = set()
    known = known_team_names()
    for label, (cid, sid) in WORLD_CUPS.items():
        matches = fetch(f"{BASE}/matches/{cid}/{sid}.json")
        print(f"WC{label}: {len(matches)} matches")
        for m in matches:
            date = m["match_date"]
            raw_home = m["home_team"]["home_team_name"]
            raw_away = m["away_team"]["away_team_name"]
            home, away = normalize(raw_home), normalize(raw_away)
            if known:
                if home not in known:
                    unmatched.add(raw_home)
                if away not in known:
                    unmatched.add(raw_away)
            lineups = fetch(f"{BASE}/lineups/{m['match_id']}.json")
            for team_obj in lineups:
                team = normalize(team_obj["team_name"])
                for player in team_obj["lineup"]:
                    rows.append((date, home, away, team,
                                 player["player_name"], 1 if is_starter(player) else 0))
    return rows, unmatched


def main():
    rows, unmatched = collect()
    out = os.path.join(_DATA, "lineups.csv")
    with open(out, "w", newline="", encoding="utf-8") as f:
        w = csv.writer(f)
        w.writerow(["match_date", "home_team", "away_team", "team", "player_name", "started"])
        w.writerows(rows)
    starters = sum(1 for r in rows if r[5] == 1)
    matches = len({(r[0], r[1], r[2]) for r in rows})
    print(f"wrote {len(rows)} player-rows across {matches} matches "
          f"({starters} starters) to {out}")
    if unmatched:
        print("\nTeam names not found in results.csv - add them to TEAM_ALIASES:")
        for t in sorted(unmatched):
            print("  ", t)


if __name__ == "__main__":
    main()

# -----------------------------------------------------------------------------
# 2006 / 2010 / 2014 are not in StatsBomb open data. There is no clean free
# structured XI feed for them. Options, roughly in order of effort:
#   1. Prove the feature on 2018 + 2022 first (this script). If it shows signal,
#      it justifies the harder backfill below.
#   2. Parse Wikipedia per-match articles (the {{football box collapsible}} /
#      lineup templates) via the MediaWiki API. Doable but fragile and needs
#      hand-checking; budget real time for it.
#   3. A licensed feed (Opta/Sportradar) or a vetted Kaggle dataset, if available.
# Phase 0 already showed 2006 squad valuations are thin, so 2006 will largely
# impute toward the squad-value baseline regardless of XI availability.
# -----------------------------------------------------------------------------
