package com.david.worldcup.goals;

import com.david.worldcup.elo.DrawModel;

import java.util.List;

/**
 * Averages the win/draw/loss probabilities of several member models. When the
 * members are comparable in accuracy but make different errors — as Elo and
 * Dixon-Coles do across different World Cups — the average is often better
 * calibrated than any single member.
 */
public final class EnsembleModel implements GoalModel {

    private final String name;
    private final List<GoalModel> members;

    public EnsembleModel(String name, List<GoalModel> members) {
        if (members.isEmpty()) {
            throw new IllegalArgumentException("ensemble needs at least one member");
        }
        this.name = name;
        this.members = List.copyOf(members);
    }

    @Override
    public DrawModel.Probabilities probabilities(String home, String away, boolean neutral) {
        double h = 0, d = 0, a = 0;
        for (GoalModel m : members) {
            DrawModel.Probabilities p = m.probabilities(home, away, neutral);
            h += p.homeWin();
            d += p.draw();
            a += p.awayWin();
        }
        int k = members.size();
        return new DrawModel.Probabilities(h / k, d / k, a / k);
    }

    @Override
    public String name() {
        return name;
    }
}
