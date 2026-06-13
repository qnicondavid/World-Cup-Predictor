package com.david.worldcup.goals;

import com.david.worldcup.value.MarketValueTable;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.Set;
import java.util.TreeSet;

/**
 * Folds squad market value into fitted {@link TeamStrength} attack/defence
 * ratings. A richer-than-average squad gets a higher attack prior and a lower
 * (better) defence prior; each team's fitted rating is shrunk toward that prior
 * by {@link ValueWeights}, with more pull for teams that have little match data.
 *
 * <p>This also lets the model rate a team with value data but <em>no</em> recent
 * matches (a debutant): such a team has no fitted rating, so it sits entirely at
 * its value-implied prior — exactly the blind spot value is meant to cover.
 */
public final class ValueAdjuster {

    private ValueAdjuster() {
    }

    public static TeamStrength adjust(TeamStrength fit, Map<String, Integer> matchCounts,
                                      MarketValueTable values, LocalDate asof, ValueWeights weights) {
        if (values.isEmpty()) {
            return fit;
        }

        // Standardise log market value across teams that have a valuation as of this date.
        Map<String, Double> logValue = new HashMap<>();
        double sum = 0.0;
        for (String team : values.teamsWithValueAsOf(asof)) {
            OptionalDouble v = values.valueAsOf(team, asof);
            if (v.isPresent() && v.getAsDouble() > 0) {
                double lv = Math.log(v.getAsDouble());
                logValue.put(team, lv);
                sum += lv;
            }
        }
        if (logValue.size() < 2) {
            return fit;
        }
        double mean = sum / logValue.size();
        double variance = 0.0;
        for (double lv : logValue.values()) {
            variance += (lv - mean) * (lv - mean);
        }
        double sd = Math.sqrt(variance / logValue.size());
        if (sd <= 0) {
            return fit;
        }

        Set<String> teams = new TreeSet<>(fit.attack().keySet());
        teams.addAll(fit.defence().keySet());
        teams.addAll(logValue.keySet());

        Map<String, Double> attack = new HashMap<>();
        Map<String, Double> defence = new HashMap<>();
        for (String team : teams) {
            double fittedAttack = fit.attackOf(team);
            double fittedDefence = fit.defenceOf(team);
            Double lv = logValue.get(team);
            if (lv == null) {
                attack.put(team, fittedAttack);
                defence.put(team, fittedDefence);
                continue;
            }
            double z = (lv - mean) / sd;
            double priorAttack = weights.valueScale() * z;
            double priorDefence = -weights.valueScale() * z;
            double w = weights.shrinkageFor(matchCounts.getOrDefault(team, 0));
            attack.put(team, (1 - w) * fittedAttack + w * priorAttack);
            defence.put(team, (1 - w) * fittedDefence + w * priorDefence);
        }
        recenter(attack);
        recenter(defence);
        return new TeamStrength(fit.baseline(), fit.homeAdvantage(), fit.rho(), attack, defence);
    }

    /** Re-centre to mean zero so attack/defence stay identifiable against the baseline. */
    private static void recenter(Map<String, Double> ratings) {
        double sum = 0.0;
        for (double v : ratings.values()) {
            sum += v;
        }
        double mean = sum / ratings.size();
        ratings.replaceAll((k, v) -> v - mean);
    }
}
