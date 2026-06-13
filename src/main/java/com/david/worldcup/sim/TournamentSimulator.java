package com.david.worldcup.sim;

import com.david.worldcup.elo.DrawModel;
import com.david.worldcup.elo.EloRatingSystem;
import com.david.worldcup.model.Fixture;
import com.david.worldcup.model.Match;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

/**
 * Monte Carlo simulation of the remainder of the 2026 World Cup.
 *
 * <p>Each run: replay the already-played group results, sample every remaining
 * group fixture from the model's win/draw/loss probabilities, build group
 * standings, qualify winners + runners-up + the best third-placed teams (filled
 * until the knockout field is a power of two — 8 thirds for the 12-group 2026
 * format), then sample the knockout bracket to a champion.
 *
 * <p>Documented simplifications:
 * <ul>
 *   <li>Group composition is inferred from the fixture list (teams that play
 *       each other in the group stage form a group) rather than hard-coded.</li>
 *   <li>Tie-breaks use points, then current Elo rating, instead of goal
 *       difference (outcomes are simulated, not scorelines).</li>
 *   <li>The knockout bracket is paired by seeding (best vs worst), which
 *       approximates FIFA's fixed bracket paths.</li>
 *   <li>Knockout matches are treated as neutral-venue with no draws: the Elo
 *       expected score is used directly as the win probability, which folds
 *       extra time and penalties into a single number.</li>
 * </ul>
 */
public final class TournamentSimulator {

    private final EloRatingSystem elo;

    public record TeamOdds(String team, double titleShare, double finalShare, double semiShare) {}

    public TournamentSimulator(EloRatingSystem elo) {
        this.elo = elo;
    }

    /** Teams grouped by who plays whom in the group stage (connected components). */
    static List<Set<String>> inferGroups(List<Match> played, List<Fixture> remaining) {
        Map<String, Set<String>> adjacency = new HashMap<>();
        for (Match m : played) {
            adjacency.computeIfAbsent(m.homeTeam(), k -> new HashSet<>()).add(m.awayTeam());
            adjacency.computeIfAbsent(m.awayTeam(), k -> new HashSet<>()).add(m.homeTeam());
        }
        for (Fixture f : remaining) {
            adjacency.computeIfAbsent(f.homeTeam(), k -> new HashSet<>()).add(f.awayTeam());
            adjacency.computeIfAbsent(f.awayTeam(), k -> new HashSet<>()).add(f.homeTeam());
        }

        List<Set<String>> groups = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        for (String team : new TreeSet<>(adjacency.keySet())) {
            if (visited.contains(team)) {
                continue;
            }
            Set<String> group = new TreeSet<>();
            List<String> stack = new ArrayList<>(List.of(team));
            while (!stack.isEmpty()) {
                String current = stack.remove(stack.size() - 1);
                if (!group.add(current)) {
                    continue;
                }
                stack.addAll(adjacency.getOrDefault(current, Set.of()));
            }
            visited.addAll(group);
            groups.add(group);
        }
        return groups;
    }

    /** Runs {@code runs} simulations and returns per-team odds, best title odds first. */
    public List<TeamOdds> simulate(List<Match> playedGroupMatches,
                                   List<Fixture> remainingGroupFixtures,
                                   int runs, long seed) {
        List<Set<String>> groups = inferGroups(playedGroupMatches, remainingGroupFixtures);
        Random random = new Random(seed);

        Map<String, int[]> tally = new LinkedHashMap<>(); // {titles, finals, semis}
        for (Set<String> g : groups) {
            for (String t : g) {
                tally.put(t, new int[3]);
            }
        }

        for (int i = 0; i < runs; i++) {
            runOnce(groups, playedGroupMatches, remainingGroupFixtures, random, tally);
        }

        List<TeamOdds> odds = new ArrayList<>();
        for (var e : tally.entrySet()) {
            odds.add(new TeamOdds(e.getKey(),
                    (double) e.getValue()[0] / runs,
                    (double) e.getValue()[1] / runs,
                    (double) e.getValue()[2] / runs));
        }
        odds.sort(Comparator.comparingDouble(TeamOdds::titleShare).reversed());
        return odds;
    }

    private void runOnce(List<Set<String>> groups, List<Match> played,
                         List<Fixture> remaining, Random random, Map<String, int[]> tally) {
        Map<String, Integer> points = new HashMap<>();

        for (Match m : played) {
            applyResult(points, m.homeTeam(), m.awayTeam(), switch (m.outcome()) {
                case HOME_WIN -> 0;
                case DRAW -> 1;
                case AWAY_WIN -> 2;
            });
        }
        for (Fixture f : remaining) {
            DrawModel.Probabilities p =
                    elo.outcomeProbabilities(f.homeTeam(), f.awayTeam(), f.neutralVenue());
            double roll = random.nextDouble();
            int outcome = roll < p.homeWin() ? 0 : (roll < p.homeWin() + p.draw() ? 1 : 2);
            applyResult(points, f.homeTeam(), f.awayTeam(), outcome);
        }

        // Rank each group: points, then current Elo (tie-break simplification).
        Comparator<String> byStrength = Comparator
                .comparingInt((String t) -> points.getOrDefault(t, 0)).reversed()
                .thenComparing(Comparator.comparingDouble(elo::ratingOf).reversed())
                .thenComparing(Comparator.naturalOrder());

        List<String> winners = new ArrayList<>();
        List<String> runners = new ArrayList<>();
        List<String> thirds = new ArrayList<>();
        for (Set<String> group : groups) {
            List<String> ranked = new ArrayList<>(group);
            ranked.sort(byStrength);
            winners.add(ranked.get(0));
            runners.add(ranked.get(1));
            if (ranked.size() > 2) {
                thirds.add(ranked.get(2));
            }
        }
        winners.sort(byStrength);
        runners.sort(byStrength);
        thirds.sort(byStrength);

        // Qualifiers: winners, runners-up, then best thirds until a power of two.
        List<String> seeds = new ArrayList<>(winners);
        seeds.addAll(runners);
        int target = Integer.highestOneBit(seeds.size()) == seeds.size()
                ? seeds.size()
                : Integer.highestOneBit(seeds.size()) * 2;
        for (String third : thirds) {
            if (seeds.size() >= target) {
                break;
            }
            seeds.add(third);
        }

        simulateKnockout(seeds, random, tally);
    }

    private void simulateKnockout(List<String> seeds, Random random, Map<String, int[]> tally) {
        List<String> alive = seeds;
        while (alive.size() > 1) {
            if (alive.size() == 4) {
                alive.forEach(t -> tally.get(t)[2]++);
            }
            if (alive.size() == 2) {
                alive.forEach(t -> tally.get(t)[1]++);
            }
            List<String> next = new ArrayList<>();
            for (int i = 0; i < alive.size() / 2; i++) {
                String a = alive.get(i);
                String b = alive.get(alive.size() - 1 - i);
                double pA = elo.winProbability(a, b, true);
                next.add(random.nextDouble() < pA ? a : b);
            }
            alive = next;
        }
        tally.get(alive.get(0))[0]++;
    }

    private static void applyResult(Map<String, Integer> points,
                                    String home, String away, int outcome) {
        points.merge(home, outcome == 0 ? 3 : (outcome == 1 ? 1 : 0), Integer::sum);
        points.merge(away, outcome == 2 ? 3 : (outcome == 1 ? 1 : 0), Integer::sum);
    }
}
