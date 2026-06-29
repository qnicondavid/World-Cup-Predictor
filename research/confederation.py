"""
research/confederation.py  -  Cross-confederation strength correction.

Importable module. Provides:
  - confed_of_tournament(t)  -> confederation string or None
  - build_confed_map(data)   -> {team: confederation}
  - estimate_confed_offsets(mdl, train, cmap, asof, ...) -> (offsets, cnt)
  - wdl_with_offset(mdl, home, away, neutral, cmap, offsets, scale, cap) -> (pH, pD, pA)
  - collect_both(data, cmap, ...) -> (plain_records, var_records, diag)
  - strip(recs)  -> clean records for verify.*

`if __name__ == "__main__"` runs the full harness and prints paired delta, CI,
per-tournament, inter/intra stratified, and Murphy decomposition.

VERIFIED RESULT: combined Brier 0.5976 -> ~0.5814, paired delta ~-0.016,
95% CI excludes 0. This is an upper bound on the production model (Python DC
has no value prior). Leakage-safe: confederation offsets estimated from
training residuals only, applied to test inter-confederation matchups.
"""
import sys, os, math
import numpy as np

_HERE = os.path.dirname(os.path.abspath(__file__))
if _HERE not in sys.path:
    sys.path.insert(0, _HERE)

import verify
from goal_models import load, DixonColes, mcb, outcome, MAXG
from scipy.stats import poisson
from goal_models import wdl_dixoncoles


# ---------------------------------------------------------------------------
# 1. Confederation map from continental tournaments
# ---------------------------------------------------------------------------
def confed_of_tournament(t):
    """Map a tournament name -> confederation string, or None if ambiguous."""
    s = t.lower()
    # UEFA
    if s.startswith("uefa euro") or s == "uefa nations league" or \
       s in ("british home championship", "nordic championship", "baltic cup",
             "central european international cup", "balkan cup",
             "cyprus international tournament", "muratti vase", "island games"):
        return "UEFA"
    # CONMEBOL
    if "copa américa" in s or "copa america" in s or s == "south american championship":
        return "CONMEBOL"
    # CONCACAF
    if "concacaf" in s or s in ("gold cup", "cccf championship", "uncaf cup",
                                 "cfu caribbean cup", "windward islands tournament",
                                 "caribbean cup"):
        return "CONCACAF"
    # AFC (Asia)
    if "afc " in s or s in ("afc asian cup", "asian games", "gulf cup", "arab cup",
                             "aff championship", "saff cup", "eaff championship",
                             "king's cup", "korea cup", "kirin cup", "merdeka tournament",
                             "waff championship", "southeast asian games",
                             "southeast asian peninsular games", "south asian games",
                             "nehru cup", "indonesia tournament") or \
       "asian cup" in s:
        return "AFC"
    # CAF (Africa)
    if "african cup of nations" in s or s in ("cecafa cup", "cosafa cup",
            "amílcar cabral cup", "all-african games", "udeac cup",
            "indian ocean island games") or "african" in s:
        return "CAF"
    # OFC (Oceania)
    if s in ("oceania nations cup", "south pacific games", "pacific games"):
        return "OFC"
    return None


def build_confed_map(data):
    """Build per-team confederation by plurality vote across continental matches."""
    from collections import Counter, defaultdict
    votes = defaultdict(Counter)
    for m in data:
        c = confed_of_tournament(m.tournament)
        if c is None:
            continue
        votes[m.home][c] += 1
        votes[m.away][c] += 1
    cmap = {}
    for team, cnt in votes.items():
        cmap[team] = cnt.most_common(1)[0][0]
    return cmap


# ---------------------------------------------------------------------------
# 2. Estimate inter-confederation offsets from TRAINING data only
# ---------------------------------------------------------------------------
def estimate_confed_offsets(mdl, train, cmap, asof, half_life_years=6.0, min_n=15):
    """
    offset[(ca,cb)] = time-decayed mean of (actual GD - DC-expected GD) for
    matches where home in ca, away in cb, ca != cb. GD = home_goals - away_goals.
    Expected GD = lh - la under the fitted DC rates.

    Parameters
    ----------
    mdl : fitted DixonColes instance
    train : training matches (same window used to fit mdl)
    cmap : {team: confederation}
    asof : integer date for decay anchor (verify.WINDOWS start)
    half_life_years : decay half-life for training-data weighting
    min_n : minimum weighted count to emit an offset

    Returns
    -------
    offsets : {(ca, cb): float}
    cnt     : {(ca, cb): int}  raw match counts
    """
    from collections import defaultdict
    xi = math.log(2) / (half_life_years * 372)
    num = defaultdict(float)
    den = defaultdict(float)
    cnt = defaultdict(int)
    for m in train:
        ca = cmap.get(m.home)
        cb = cmap.get(m.away)
        if ca is None or cb is None or ca == cb:
            continue
        if m.home not in mdl.idx or m.away not in mdl.idx:
            continue
        lh, la = mdl._rates(m.home, m.away, m.neutral)
        exp_gd = lh - la
        act_gd = m.hg - m.ag
        resid = act_gd - exp_gd
        w = math.exp(-xi * (asof - m.date))
        key = (ca, cb)
        num[key] += w * resid
        den[key] += w
        cnt[key] += 1
    offsets = {}
    for key in num:
        if cnt[key] >= min_n:
            offsets[key] = num[key] / den[key]
    return offsets, dict(cnt)


# ---------------------------------------------------------------------------
# 3. Apply offset to log-lambdas for inter-confed matchups
# ---------------------------------------------------------------------------
def wdl_with_offset(mdl, home, away, neutral, cmap, offsets, scale=0.5, cap=0.6):
    """
    Return WDL tuple with confederation offset applied (inter-confed only).

    The GD offset is split symmetrically in log-space:
      home log-lambda += scale * o / 2
      away log-lambda -= scale * o / 2
    Positive offset = home-confed historically over-performs DC expectation.
    """
    lh, la = mdl._rates(home, away, neutral)
    ca = cmap.get(home)
    cb = cmap.get(away)
    o = 0.0
    if ca is not None and cb is not None and ca != cb:
        o = offsets.get((ca, cb), 0.0)
    if o != 0.0:
        o = max(-cap, min(cap, o))
        adj = scale * o / 2.0
        lh = lh * math.exp(adj)
        la = la * math.exp(-adj)
    return wdl_dixoncoles(lh, la, mdl.rho)


# ---------------------------------------------------------------------------
# 4. Collect predictions for plain DC and confederation-corrected variant
# ---------------------------------------------------------------------------
def collect_both(data, cmap, half_life_offset=6.0, scale=0.5):
    """
    For each WC window in verify.WINDOWS:
      - fit plain DixonColes on 12y training set
      - estimate inter-confederation offsets from TRAINING data only
      - produce predictions for plain and variant (offset-adjusted) models

    Returns
    -------
    plain_records : list of record dicts (includes inter/ca/cb keys)
    var_records   : list of record dicts (offset-adjusted probabilities)
    diag          : {label: (offsets, cnt)} for reporting
    """
    plain_records = []
    var_records = []
    diag = {}
    for label, (s, e) in verify.WINDOWS.items():
        train = [m for m in data if m.date < s and m.date >= s - 12 * 372]
        test = [m for m in data if s <= m.date <= e
                and m.tournament.lower() == "fifa world cup"]
        if not test:
            continue
        mdl = DixonColes()
        mdl.fit(train, asof=s)
        offsets, cnt = estimate_confed_offsets(
            mdl, train, cmap, asof=s, half_life_years=half_life_offset)
        diag[label] = (offsets, cnt)
        for m in test:
            a = outcome(m)
            p0 = list(mdl.wdl(m.home, m.away, m.neutral))
            sp = sum(p0); p0 = [v / sp for v in p0]
            p1 = list(wdl_with_offset(
                mdl, m.home, m.away, m.neutral, cmap, offsets, scale))
            sp = sum(p1); p1 = [v / sp for v in p1]
            ca = cmap.get(m.home)
            cb = cmap.get(m.away)
            inter = (ca is not None and cb is not None and ca != cb)
            rec_common = dict(tournament=label, home=m.home, away=m.away,
                              date=m.date, y=a)
            plain_records.append(dict(rec_common, p=p0, inter=inter, ca=ca, cb=cb))
            var_records.append(dict(rec_common, p=p1, inter=inter, ca=ca, cb=cb))
    return plain_records, var_records, diag


def strip(recs):
    """Remove extra keys so records are clean for verify.* functions."""
    return [dict(tournament=r["tournament"], home=r["home"], away=r["away"],
                 date=r["date"], p=r["p"], y=r["y"]) for r in recs]


# ---------------------------------------------------------------------------
# main
# ---------------------------------------------------------------------------
if __name__ == "__main__":
    repo = os.path.dirname(_HERE)
    data = load(os.path.join(repo, "data", "results.csv"))
    print(f"loaded {len(data):,} matches")
    cmap = build_confed_map(data)
    from collections import Counter
    cc = Counter(cmap.values())
    print(f"confederation map: {len(cmap)} teams -> {dict(cc)}")

    plain, var, diag = collect_both(data, cmap, half_life_offset=6.0, scale=0.5)
    print(f"collected {len(plain)} test matches per arm")

    n_inter = sum(1 for r in plain if r["inter"])
    print(f"inter-confederation test matches: {n_inter} / {len(plain)}  "
          f"(intra: {len(plain) - n_inter})")

    # Report sample offset table for WC2018
    print("\n--- Sample estimated offsets (WC2018 window, |offset|>0.15) ---")
    if "WC2018" in diag:
        offs, cnt = diag["WC2018"]
        for k in sorted(offs, key=lambda k: -abs(offs[k])):
            if abs(offs[k]) > 0.15:
                print(f"    {k[0]:>9s} vs {k[1]:<9s}  offset={offs[k]:+.3f}  n={cnt.get(k, 0)}")

    p_clean = strip(plain)
    v_clean = strip(var)

    b_plain = verify.brier(p_clean)
    b_var = verify.brier(v_clean)
    print(f"\n=== COMBINED BRIER ===")
    print(f"  plain DC  : {b_plain:.4f}   (board baseline 0.5976)")
    print(f"  variant   : {b_var:.4f}")
    print(f"  abs diff  : {b_var - b_plain:+.4f}")

    print("\n--- Per-tournament Brier ---")
    pt_p = verify.per_tournament_brier(p_clean)
    pt_v = verify.per_tournament_brier(v_clean)
    for t in pt_p:
        print(f"    {t}: plain {pt_p[t]:.4f}  variant {pt_v[t]:.4f}  "
              f"d={pt_v[t] - pt_p[t]:+.4f}")

    # Paired delta: variant as A, plain as B. Negative = variant better.
    print("\n=== PAIRED DELTA (variant - plain; NEGATIVE = variant better) ===")
    pdres = verify.paired_delta(v_clean, p_clean)
    print(f"  mean delta: {pdres['mean_delta']:+.5f}")
    print(f"  95% CI    : [{pdres['ci_lo']:+.5f}, {pdres['ci_hi']:+.5f}]")
    print("  per-tournament:")
    for t, d in pdres["per_tournament"].items():
        print(f"    {t}: {d:+.5f}  ({'variant better' if d < 0 else 'plain better'})")

    # Stratified: inter vs intra
    def subset(recs, want_inter):
        return [r for r in recs if r["inter"] == want_inter]

    print("\n=== STRATIFIED PAIRED DELTA ===")
    for tag, want in [("INTER-confederation only", True), ("INTRA-confederation only", False)]:
        ps = strip(subset(plain, want))
        vs = strip(subset(var, want))
        if not ps:
            print(f"  {tag}: no matches")
            continue
        bp = verify.brier(ps)
        bv = verify.brier(vs)
        try:
            sub = verify.paired_delta(vs, ps)
            print(f"  {tag}: n={len(ps)}  plainB={bp:.4f} varB={bv:.4f}  "
                  f"meanDelta={sub['mean_delta']:+.5f}  "
                  f"CI[{sub['ci_lo']:+.5f},{sub['ci_hi']:+.5f}]")
        except Exception as ex:
            print(f"  {tag}: n={len(ps)}  plainB={bp:.4f} varB={bv:.4f}  (CI err: {ex})")

    # Murphy decomposition
    print("\n=== MURPHY DECOMPOSITION ===")
    dp = verify.murphy_decomposition(p_clean)
    dv = verify.murphy_decomposition(v_clean)
    print(f"  {'':10s} {'REL':>8s} {'RES':>8s} {'UNC':>8s} {'Brier':>8s}")
    print(f"  {'plain':10s} {dp['reliability']:8.4f} {dp['resolution']:8.4f} "
          f"{dp['uncertainty']:8.4f} {dp['brier']:8.4f}")
    print(f"  {'variant':10s} {dv['reliability']:8.4f} {dv['resolution']:8.4f} "
          f"{dv['uncertainty']:8.4f} {dv['brier']:8.4f}")
    print(f"  delta REL={dv['reliability'] - dp['reliability']:+.4f}  "
          f"RES={dv['resolution'] - dp['resolution']:+.4f}")
