package com.david.worldcup.goals;

import com.david.worldcup.goals.Calibration.Outcome;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CalibrationTest {

    @Test
    void temperatureOneIsIdentityAndOutputsNormalise() {
        double[] p = {0.7, 0.2, 0.1};
        double[] q = Calibration.applyTemperature(p, 1.0);
        assertEquals(0.7, q[0], 1e-9);
        assertEquals(0.2, q[1], 1e-9);
        assertEquals(1.0, q[0] + q[1] + q[2], 1e-9);
    }

    @Test
    void highTemperatureFlattensTowardUniform() {
        double[] q = Calibration.applyTemperature(new double[] {0.7, 0.2, 0.1}, 5.0);
        assertTrue(q[0] < 0.7, "peak should drop");
        assertTrue(q[2] > 0.1, "tail should rise");
        assertEquals(1.0, q[0] + q[1] + q[2], 1e-9);
    }

    @Test
    void brierAndLogLossMatchManualValues() {
        List<Outcome> one = List.of(new Outcome(new double[] {0.8, 0.1, 0.1}, 0));
        assertEquals(0.06, Calibration.brier(one), 1e-9);          // .04 + .01 + .01
        assertEquals(-Math.log(0.8), Calibration.logLoss(one), 1e-9);
    }

    @Test
    void overconfidentModelWantsSofterTemperature() {
        // claims 90% home, but home happens only 60% of the time
        List<Outcome> data = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            data.add(new Outcome(new double[] {0.9, 0.05, 0.05}, 0));
        }
        for (int i = 0; i < 4; i++) {
            data.add(new Outcome(new double[] {0.9, 0.05, 0.05}, 1));
        }
        assertTrue(Calibration.fitTemperature(data) > 1.0);
    }

    @Test
    void underconfidentModelWantsSharperTemperature() {
        // claims 40% home, but home happens 80% of the time
        List<Outcome> data = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            data.add(new Outcome(new double[] {0.4, 0.3, 0.3}, 0));
        }
        for (int i = 0; i < 2; i++) {
            data.add(new Outcome(new double[] {0.4, 0.3, 0.3}, 1));
        }
        assertTrue(Calibration.fitTemperature(data) < 1.0);
    }

    @Test
    void reliabilityMatchesObservedFrequency() {
        // class 0 always predicted 0.5 and occurs exactly half the time
        List<Outcome> data = List.of(
                new Outcome(new double[] {0.5, 0.25, 0.25}, 0),
                new Outcome(new double[] {0.5, 0.25, 0.25}, 0),
                new Outcome(new double[] {0.5, 0.25, 0.25}, 1),
                new Outcome(new double[] {0.5, 0.25, 0.25}, 2));
        Calibration.Bin halfBin = Calibration.reliability(data, 10).stream()
                .filter(b -> b.low() <= 0.5 && 0.5 < b.high())
                .findFirst().orElseThrow();
        assertEquals(0.5, halfBin.meanPredicted(), 1e-9);
        assertEquals(0.5, halfBin.observedFrequency(), 1e-9); // 2 of 4 were home
    }
}
