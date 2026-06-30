#!/usr/bin/env python3
"""
research/fetch_odds_2022.py - pull WC 2022 closing 1X2 odds from The Odds API
historical endpoint and write them in the odds_history.csv schema.

The historical endpoint needs a PAID Odds API plan and costs ~10 credits per
market/region per call. This script:
  1. enumerates the tournament's events (id, kickoff, teams) by walking the
     historical /events endpoint across the WC-2022 window, then
  2. for each event, fetches the odds snapshot nearest kickoff (= closing line),
     averages the h2h prices across EU bookmakers, and writes a row to
     data/odds_history_2022.csv:
         match_date,home_team,away_team,home_odds,draw_odds,away_odds

The key is read from ODDS_API_KEY (or research/.odds_api_key) and is NEVER
printed or committed. Run a dry estimate first, then add --confirm to spend:

    python3 research/fetch_odds_2022.py            # dry run: prints call/credit estimate
    python3 research/fetch_odds_2022.py --confirm  # actually fetches (spends credits)

Docs: https://the-odds-api.com/liveapi/guides/v4/#get-historical-odds
"""
import csv
import json
import os
import sys
import time
import urllib.error
import urllib.parse
import urllib.request

_HERE = os.path.dirname(os.path.abspath(__file__))
_ROOT = os.path.join(_HERE, "..")
BASE = "https://api.the-odds-api.com/v4"
SPORT = "soccer_fifa_world_cup"
# WC 2022: 20 Nov - 18 Dec 2022. Walk the window every 2 days at noon UTC to
# harvest the full event list; odds appear days before kickoff so this is ample.
WINDOW_START = "2022-11-18"
WINDOW_END = "2022-12-19"
ENUM_STEP_DAYS = 2
REGION, MARKET = "eu", "h2h"


def read_key():
    key = os.environ.get("ODDS_API_KEY")
    if not key:
        p = os.path.join(_HERE, ".odds_api_key")
        if os.path.exists(p):
            key = open(p, encoding="utf-8").read().strip()
    if not key:
        sys.exit("Set ODDS_API_KEY (or research/.odds_api_key). Never printed/committed.")
    return key


def api_get(path, key, **params):
    params["apiKey"] = key
    url = path + "?" + urllib.parse.urlencode(params)
    with urllib.request.urlopen(url, timeout=60) as r:
        used = r.headers.get("x-requests-used")
        remaining = r.headers.get("x-requests-remaining")
        data = json.load(r)
    return data, used, remaining


def daterange(start, end, step):
    import datetime as dt
    d = dt.date.fromisoformat(start)
    last = dt.date.fromisoformat(end)
    while d <= last:
        yield d.isoformat()
        d += dt.timedelta(days=step)


def enumerate_events(key):
    """Walk historical /events across the window; dedup by event id."""
    events = {}
    for day in daterange(WINDOW_START, WINDOW_END, ENUM_STEP_DAYS):
        snap = f"{day}T12:00:00Z"
        try:
            payload, used, remaining = api_get(
                f"{BASE}/historical/sports/{SPORT}/events", key, date=snap)
        except urllib.error.HTTPError as e:
            print(f"  events @ {snap}: HTTP {e.code} ({e.reason}) - skipping")
            continue
        data = payload.get("data", payload if isinstance(payload, list) else [])
        for ev in data:
            events[ev["id"]] = {"commence_time": ev["commence_time"],
                                "home": ev["home_team"], "away": ev["away_team"]}
        print(f"  events @ {snap}: {len(data)} live, {len(events)} unique so far "
              f"(used {used}, remaining {remaining})")
    return events


def avg_h2h(bookmakers, home, away):
    h, d, a = [], [], []
    for bk in bookmakers:
        for m in bk.get("markets", []):
            if m["key"] != "h2h":
                continue
            for oc in m["outcomes"]:
                if oc["name"] == home:
                    h.append(oc["price"])
                elif oc["name"] == away:
                    a.append(oc["price"])
                elif oc["name"] == "Draw":
                    d.append(oc["price"])
    if h and d and a:
        return sum(h) / len(h), sum(d) / len(d), sum(a) / len(a)
    return None


def main():
    confirm = "--confirm" in sys.argv
    key = read_key()

    print("Phase 1: enumerating WC-2022 events (cheap historical /events calls)...")
    events = enumerate_events(key)
    n = len(events)
    print(f"found {n} unique events.")
    if not confirm:
        n_enum = len(list(daterange(WINDOW_START, WINDOW_END, ENUM_STEP_DAYS)))
        print(f"\nDRY RUN. To fetch closing odds for {n} events you will spend roughly:")
        print(f"  Phase 1: {n_enum} historical /events calls")
        print(f"  Phase 2: {n} historical per-event odds calls "
              f"(~10 credits each = ~{n*10} credits)")
        print("Re-run with --confirm to actually fetch. (Your paid plan is charged.)")
        return

    print("\nPhase 2: fetching each event's closing snapshot (nearest kickoff)...")
    rows = []
    for i, (eid, ev) in enumerate(sorted(events.items(), key=lambda kv: kv[1]["commence_time"]), 1):
        try:
            payload, used, remaining = api_get(
                f"{BASE}/historical/sports/{SPORT}/events/{eid}/odds", key,
                date=ev["commence_time"], regions=REGION, markets=MARKET,
                oddsFormat="decimal")
        except urllib.error.HTTPError as e:
            print(f"  [{i}/{n}] {ev['home']} v {ev['away']}: HTTP {e.code} - skipped")
            continue
        d = payload.get("data") or {}
        avg = avg_h2h(d.get("bookmakers", []), ev["home"], ev["away"])
        date = ev["commence_time"][:10]
        if avg:
            rows.append([date, ev["home"], ev["away"],
                         f"{avg[0]:.4f}", f"{avg[1]:.4f}", f"{avg[2]:.4f}"])
            print(f"  [{i}/{n}] {date} {ev['home']} v {ev['away']}: "
                  f"{avg[0]:.2f}/{avg[1]:.2f}/{avg[2]:.2f} (remaining {remaining})")
        else:
            print(f"  [{i}/{n}] {date} {ev['home']} v {ev['away']}: no h2h odds in snapshot")
        time.sleep(0.3)

    out = os.path.join(_ROOT, "data", "odds_history_2022.csv")
    with open(out, "w", newline="", encoding="utf-8") as f:
        w = csv.writer(f)
        w.writerow(["match_date", "home_team", "away_team",
                    "home_odds", "draw_odds", "away_odds"])
        w.writerows(rows)
    print(f"\nwrote {len(rows)} matches -> {out}")
    print("Next: python3 research/ingest_odds_history.py")


if __name__ == "__main__":
    main()
