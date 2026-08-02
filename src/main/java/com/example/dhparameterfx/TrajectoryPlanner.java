package com.example.dhparameterfx;

import java.util.ArrayList;
import java.util.List;

public class TrajectoryPlanner {

    public static class Waypoint {
        public final double[] jointAngles;
        public Waypoint(double[] angles) {
            this.jointAngles = angles.clone();

        }
    }

    /**
     * Interpolates joint angles at normalized time t in [0, 1].
     */
    public static double[] interpolateCubic(double[] qStart, double[] qEnd, double tNorm) {

        tNorm = Math.max(0.0, Math.min(1.0, tNorm)); // Clamp to [0, 1]

        // Cubic blend polynomial s(t) = 3t^2 - 2t^3
        double s = 3 * tNorm * tNorm - 2 * tNorm * tNorm * tNorm;

        double[] qOut = new double[qStart.length];

        for (int i = 0; i < qStart.length; i++) {

            qOut[i] = qStart[i] + s * (qEnd[i] - qStart[i]);

        }
        return qOut;

    }
}