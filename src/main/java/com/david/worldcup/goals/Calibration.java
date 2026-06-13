package com.david.worldcup.goals;

import java.util.ArrayList;
import java.util.List;

/**
 * Calibration diagnostics for three-way (home/draw/away) probability forecasts.
 *
 * <p>A model is <em>calibrated</em> if, among the matches it gives a 30% chance,
 * about 30% actually happen. Good Brier does not guarantee this — a model can be
 * systematically over- or under-confident — and betting value is very sensitive
 * to it, so we measure it directly:
 * <ul>
 *   <li><b>Reliability bins</b> — pooling all (match, outcome) pairs one-vs-rest,
 *       bin by predicted probability and compare the mean prediction to the
 *       observed frequency in each bin.</li>
 *   <li><b>ECE</b> — expected calibration error, the count-weighted average gap
 *       between predicted and observed across bins (0 = perfectly calibrated).</li>
 *   <li><b>Log-loss</b> — penalises confident mistakes harder than Brier.</li>
 * </ul>
 *
 * <p>If the model is mis-calibrated, {@link #fitTemperature} finds a single
 * temperature {@code T} that rescales probabilities ({@code p_i^(1/T)},
 * renormalised): {@code T>1} softens an over-confident model, {@code T<1}
 * sharpens an under-confident one.
 */
public final class Calibration {

    private Calibration() {
    }

    /** One scored forecast: the three probabilities and the actual outcome index (0/1/2). */
    public record Outcome(double[] probs, int actual) {}

    /** A reliability bin over predicted probability [low, high). */
    public record Bin(double low, double high, double meanPredicted,
                      double observedFrequency, int count) {}

    public static List<Bin> reliability(List<Outcome> data, int binCount) {
        double[] sumPredicted = new double[binCount];
        double[] sumHits = new double[binCount];
        int[] counts = new int[binCount];
        for (Outcome o : data) {
            for (int c = 0; c < o.probs().length; c++) {
                double p = o.probs()[c];
                int b = (int) Math.floor(p * binCount);
                b = Math.max(0, Math.min(binCount - 1, b));
                sumPredicted[b] += p;
                sumHits[b] += (c == o.actual()) ? 1.0 : 0.0;
                counts[b]++;
            }
        }
        List<Bin> bins = new ArrayList<>();
        for (int b = 0; b < binCount; b++) {
            if (counts[b] == 0) {
                continue;
            }
            bins.add(new Bin((double) b / binCount, (double) (b + 1) / binCount,
                    sumPredicted[b] / counts[b], sumHits[b] / counts[b], counts[b]));
        }
        return bins;
    }

    /** Count-weighted mean gap between predicted and observed across bins. */
    public static double expectedCalibrationError(List<Outcome> data, int binCount) {
        List<Bin> bins = reliability(data, binCount);
        int total = 0;
        for (Bin bin : bins) {
            total += bin.count();
        }
        if (total == 0) {
            return 0.0;
        }
        double ece = 0.0;
        for (Bin bin : bins) {
            ece += (double) bin.count() / total
                    * Math.abs(bin.meanPredicted() - bin.observedFrequency());
        }
        return ece;
    }

    /** Mean negative log probability assigned to the actual outcome. */
    public static double logLoss(List<Outcome> data) {
        if (data.isEmpty()) {
            return 0.0;
        }
        double sum = 0.0;
        for (Outcome o : data) {
            sum += -Math.log(Math.max(1e-15, o.probs()[o.actual()]));
        }
        return sum / data.size();
    }

    /** Mean multiclass Brier score (sum of squared error over the three outcomes). */
    public static double brier(List<Outcome> data) {
        if (data.isEmpty()) {
            return 0.0;
        }
        double sum = 0.0;
        for (Outcome o : data) {
            for (int c = 0; c < o.probs().length; c++) {
                double target = c == o.actual() ? 1.0 : 0.0;
                double diff = o.probs()[c] - target;
                sum += diff * diff;
            }
        }
        return sum / data.size();
    }

    /** Temperature-rescale a probability vector: {@code p_i^(1/T)}, renormalised. */
    public static double[] applyTemperature(double[] probs, double temperature) {
        double[] q = new double[probs.length];
        double sum = 0.0;
        for (int i = 0; i < probs.length; i++) {
            q[i] = Math.pow(Math.max(probs[i], 0.0), 1.0 / temperature);
            sum += q[i];
        }
        for (int i = 0; i < q.length; i++) {
            q[i] = sum > 0 ? q[i] / sum : 1.0 / q.length;
        }
        return q;
    }

    public static List<Outcome> rescale(List<Outcome> data, double temperature) {
        List<Outcome> out = new ArrayList<>(data.size());
        for (Outcome o : data) {
            out.add(new Outcome(applyTemperature(o.probs(), temperature), o.actual()));
        }
        return out;
    }

    /** Temperature in [0.5, 2.0] minimising log-loss on {@code data}. */
    public static double fitTemperature(List<Outcome> data) {
        double best = 1.0;
        double bestLoss = Double.MAX_VALUE;
        for (int step = 0; step <= 30; step++) {
            double t = 0.5 + step * 0.05;
            double loss = logLoss(rescale(data, t));
            if (loss < bestLoss) {
                bestLoss = loss;
                best = t;
            }
        }
        return best;
    }
}
