"""
research/recalibrate.py  -  Leakage-safe recalibration of WDL probabilities.

Charge (Raghavan / "The Bayesian"):
  The Dixon-Coles model over-predicts DRAW (mean 0.271 vs realized 0.225) and is
  under-confident on HOME in the mid/strong-favourite range. Weiss localized the
  loss to the moderate-favourite band; Tan localized it to CALIBRATION (reliability)
  on HOME/DRAW, and showed a single global temperature does NOT transfer.

  This module prototypes 4 recalibration families, each fit by maximum likelihood
  (log-loss) on TRAIN tournaments only, and evaluated LEAVE-ONE-TOURNAMENT-OUT.

Transforms operate on logits z = log(p) (class-shared additive constant is free):
  (a) global temperature   : p' ~ softmax(z / T)                         [1 param]
  (b) class vector-scaling : p' ~ softmax(a_c * z_c + b_c)               [up to 6]
       - we use the leakage-safe minimal variant: per-class bias only,
         p' ~ softmax(z_c + b_c)  with b summing-free (b_home fixed 0)    [2 params]
       - and a temperature+draw-bias variant: p' ~ softmax(z/T + b_draw) [2 params]
  (c) favourite-band shrinkage : near even-money games, pull toward the
       observed (train) prior; strength = sigmoid distance from 0.5      [2 params]
  (d) Dirichlet / full vector scaling p' ~ softmax(a_c z_c + b_c)        [4-6 params]

Run:  python3 research/recalibrate.py
"""
import sys, os, math
import numpy as np

_HERE = os.path.dirname(os.path.abspath(__file__))
if _HERE not in sys.path:
    sys.path.insert(0, _HERE)
from verify import brier, per_tournament_brier, murphy_decomposition, block_bootstrap

CSV = os.path.join(_HERE, "export_predictions_form.csv")
AMAP = {"home": 0, "draw": 1, "away": 2}
EPS = 1e-9


def load_records(path=CSV):
    import csv as csvmod
    recs = []
    with open(path, newline="", encoding="utf-8") as fh:
        for row in csvmod.DictReader(fh):
            p = [float(row["p_home"]), float(row["p_draw"]), float(row["p_away"])]
            s = sum(p); p = [v / s for v in p]
            recs.append(dict(tournament=row["tournament"].strip(),
                             home=row["home"].strip(), away=row["away"].strip(),
                             date=row["date"].strip(), p=p, y=AMAP[row["actual"].strip()]))
    return recs


def _P(recs):
    return np.array([r["p"] for r in recs])


def _Y(recs):
    return np.array([r["y"] for r in recs])


def _logits(P):
    return np.log(np.clip(P, EPS, 1.0))


def _softmax(Z):
    Z = Z - Z.max(axis=1, keepdims=True)
    E = np.exp(Z)
    return E / E.sum(axis=1, keepdims=True)


def _logloss(P, Y):
    return -np.mean(np.log(np.clip(P[np.arange(len(Y)), Y], EPS, 1.0)))


# ---------------------------------------------------------------------------
# Transform families.  Each: apply(P, theta) -> P'.  fit(P,Y) -> theta (ML).
# theta is an unconstrained vector; we minimise train log-loss by coordinate
# grid + local refine (no scipy dependency; few params so this is robust).
# ---------------------------------------------------------------------------
class Family:
    name = "base"
    n = 0
    def apply(self, P, theta): raise NotImplementedError
    def grids(self): raise NotImplementedError   # list of 1-D grids, one per param
    def fit(self, P, Y):
        Z = _logits(P)
        grids = self.grids()
        # coarse exhaustive over product of grids (params are few)
        best, best_ll = None, math.inf
        import itertools
        for combo in itertools.product(*grids):
            theta = np.array(combo, float)
            ll = _logloss(self._apply_Z(Z, theta), Y)
            if ll < best_ll:
                best_ll, best = ll, theta
        # local refine: coordinate descent with shrinking step
        theta = best.copy().astype(float)
        step = np.array([max(1e-3, (g[1] - g[0]) if len(g) > 1 else 0.1) for g in grids])
        for _ in range(60):
            improved = False
            for i in range(len(theta)):
                for d in (+1, -1):
                    cand = theta.copy(); cand[i] += d * step[i]
                    ll = _logloss(self._apply_Z(Z, cand), Y)
                    if ll < best_ll - 1e-12:
                        best_ll, theta, improved = ll, cand, True
            if not improved:
                step *= 0.5
                if np.all(step < 1e-4): break
        return theta
    def _apply_Z(self, Z, theta): raise NotImplementedError
    def apply(self, P, theta):
        return self._apply_Z(_logits(P), theta)


class Identity(Family):
    name = "identity"; n = 0
    def grids(self): return [np.array([0.0])]
    def _apply_Z(self, Z, theta): return _softmax(Z)
    def fit(self, P, Y): return np.array([0.0])


class GlobalTemp(Family):
    # (a) p' ~ softmax(z / T)
    name = "global_temp"; n = 1
    def grids(self): return [np.linspace(0.5, 2.5, 21)]
    def _apply_Z(self, Z, theta):
        T = max(theta[0], 0.05)
        return _softmax(Z / T)


class TempDrawBias(Family):
    # (b-lite) p' ~ softmax(z / T + [0, b_draw, 0])  -- 2 params
    name = "temp_drawbias"; n = 2
    def grids(self): return [np.linspace(0.6, 2.0, 15), np.linspace(-0.8, 0.4, 13)]
    def _apply_Z(self, Z, theta):
        T = max(theta[0], 0.05); bD = theta[1]
        return _softmax(Z / T + np.array([0.0, bD, 0.0]))


class DrawBias(Family):
    # WINNER: single-parameter draw-logit deflation.
    # p' ~ softmax(log p + [0, b_draw, 0]).  b_draw < 0 removes draw mass,
    # redistributing it to home/away in proportion to their current odds.
    name = "draw_bias"; n = 1
    def grids(self): return [np.linspace(-0.8, 0.4, 25)]
    def _apply_Z(self, Z, theta):
        return _softmax(Z + np.array([0.0, theta[0], 0.0]))


class ClassBias(Family):
    # (b) per-class additive bias, home fixed at 0 (identifiable): 2 params
    name = "class_bias"; n = 2
    def grids(self): return [np.linspace(-0.8, 0.6, 15), np.linspace(-0.8, 0.6, 15)]
    def _apply_Z(self, Z, theta):
        # theta = [b_draw, b_away]; b_home = 0
        b = np.array([0.0, theta[0], theta[1]])
        return _softmax(Z + b)


class VectorScale(Family):
    # (d) full vector scaling per class: a_c*z_c + b_c, home a fixed 1, b fixed 0
    # params: a_draw, a_away, b_draw, b_away  (4 params)
    name = "vector_scale"; n = 4
    def grids(self):
        return [np.linspace(0.6, 1.6, 6), np.linspace(0.6, 1.6, 6),
                np.linspace(-0.6, 0.4, 6), np.linspace(-0.6, 0.4, 6)]
    def _apply_Z(self, Z, theta):
        a = np.array([1.0, theta[0], theta[1]])
        b = np.array([0.0, theta[2], theta[3]])
        return _softmax(a * Z + b)


class FavBandShrink(Family):
    # (c) favourite-band-aware shrinkage toward the train prior.
    # weight w(x) = lam * exp(-((fav-0.5)/s)^2), fav = max(pH,pA).
    # p' = (1-w) p + w * prior   then renormalise.  params: lam, s.
    # prior is the TRAIN base-rate, stored at fit time (leakage-safe).
    name = "fav_band_shrink"; n = 2
    def __init__(self): self.prior = np.array([1/3, 1/3, 1/3])
    def grids(self): return [np.linspace(0.0, 0.9, 10), np.linspace(0.05, 0.5, 10)]
    def fit(self, P, Y):
        self.prior = np.array([(Y == c).mean() for c in range(3)])
        return super().fit(P, Y)
    def _apply_Z(self, Z, theta):
        P = _softmax(Z)
        lam = min(max(theta[0], 0.0), 1.0); s = max(theta[1], 1e-3)
        fav = np.maximum(P[:, 0], P[:, 2])
        w = lam * np.exp(-((fav - 0.5) / s) ** 2)
        Pp = (1 - w)[:, None] * P + w[:, None] * self.prior[None, :]
        return Pp / Pp.sum(axis=1, keepdims=True)
    def apply(self, P, theta):
        return self._apply_Z(_logits(P), theta)


FAMILIES = [Identity, GlobalTemp, DrawBias, TempDrawBias, ClassBias, VectorScale, FavBandShrink]


# ---------------------------------------------------------------------------
# LOTO: fit on train tournaments, apply to held-out tournament.
# ---------------------------------------------------------------------------
def loto_recalibrate(records, family_cls):
    tours = sorted({r["tournament"] for r in records})
    out = []          # recalibrated records (held-out only, all tournaments concatenated)
    thetas = {}
    for held in tours:
        train = [r for r in records if r["tournament"] != held]
        test = [r for r in records if r["tournament"] == held]
        fam = family_cls()
        theta = fam.fit(_P(train), _Y(train))
        thetas[held] = (fam, theta)
        Ptest = fam.apply(_P(test), theta)
        for i, r in enumerate(test):
            out.append(dict(r, p=[float(v) for v in Ptest[i]]))
    return out, thetas


def main():
    recs = load_records()
    print(f"Loaded {len(recs)} records, {len(set(r['tournament'] for r in recs))} tournaments")
    base_ptb = per_tournament_brier(recs)
    base_pool = brier(recs)
    print(f"\nBASELINE pooled Brier: {base_pool:.4f}")
    for t, b in base_ptb.items():
        print(f"  {t}: {b:.4f}")

    print("\n=== LOTO recalibration by family ===")
    print(f"{'family':16s} {'pooled':>8s} {'improve':>9s}  per-tournament-improvement")
    results = {}
    for fam_cls in FAMILIES:
        recal, thetas = loto_recalibrate(recs, fam_cls)
        ptb = per_tournament_brier(recal)
        pool = brier(recal)
        imp = base_pool - pool
        per_imp = {t: base_ptb[t] - ptb[t] for t in base_ptb}
        results[fam_cls.name] = (recal, pool, imp, per_imp, thetas)
        signs = " ".join(f"{t[-2:]}:{per_imp[t]:+.4f}" for t in sorted(per_imp))
        n_pos = sum(1 for v in per_imp.values() if v > 0)
        print(f"{fam_cls.name:16s} {pool:8.4f} {imp:+9.4f}  [{n_pos}/5 +] {signs}")

    # pick best by pooled improvement
    best_name = max(results, key=lambda k: results[k][2])
    recal, pool, imp, per_imp, thetas = results[best_name]
    print(f"\n=== BEST family: {best_name} ===")
    print(f"Pooled: baseline {base_pool:.4f} -> {pool:.4f}  (improvement {imp:+.4f})")
    print("Per-tournament before/after:")
    after = per_tournament_brier(recal)
    for t in sorted(base_ptb):
        print(f"  {t}: {base_ptb[t]:.4f} -> {after[t]:.4f}  ({base_ptb[t]-after[t]:+.4f})")

    # paired bootstrap CI on the improvement (resample tournaments)
    base_by = {(r['tournament'], r['home'], r['away'], r['date']): r for r in recs}
    paired = []
    from verify import mcb
    for r in recal:
        k = (r['tournament'], r['home'], r['away'], r['date'])
        b0 = mcb(base_by[k]['p'], base_by[k]['y'])
        b1 = mcb(r['p'], r['y'])
        paired.append(dict(tournament=r['tournament'], home=r['home'], away=r['away'],
                           date=r['date'], p=[b0 - b1], y=0, _d=b0 - b1))
    pt, lo, hi = block_bootstrap(paired, lambda rs: float(np.mean([x['_d'] for x in rs])))
    print(f"\nImprovement (baseline - recal), block-bootstrap over tournaments:")
    print(f"  mean {pt:+.4f}  95% CI [{lo:+.4f}, {hi:+.4f}]")

    # Murphy decomposition before/after
    d0 = murphy_decomposition(recs); d1 = murphy_decomposition(recal)
    print(f"\nMurphy:  REL {d0['reliability']:.4f} -> {d1['reliability']:.4f}   "
          f"RES {d0['resolution']:.4f} -> {d1['resolution']:.4f}")

    # Gate
    n_pos = sum(1 for v in per_imp.values() if v > 0)
    gate = (imp >= 0.006) and (n_pos >= 4) and (lo > 0)
    print(f"\n=== GATE: imp>=0.006 ({imp:+.4f}), consistent>=4/5 ({n_pos}/5), CI>0 (lo={lo:+.4f}) ===")
    print(f"VERDICT: {'PASS' if gate else 'FAIL'}")
    # fitted parameter ranges across folds for the winner
    print(f"\nFitted parameters for {best_name} across the 5 LOTO folds:")
    for held, (fam, theta) in thetas.items():
        line = f"  hold-out {held}: theta = {np.round(theta,4).tolist()}"
        if hasattr(fam, 'prior'):
            line += f"  prior={np.round(fam.prior,4).tolist()}"
        print(line)
    return results


if __name__ == "__main__":
    main()
