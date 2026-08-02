package com.example.dhparameterfx;

public class TrajectoryPlanner {

    /**
     * Interpolates joint angles using a smooth cubic polynomial.
     * Shortest-path wrapping has been removed to respect absolute joint limits.
     */
    public static double[] interpolateCubic(double[] qStart, double[] qEnd, double tNorm) {
        tNorm = Math.max(0.0, Math.min(1.0, tNorm));

        // Smooth cubic velocity blend s(t) = 3t^2 - 2t^3
        double s = 3 * tNorm * tNorm - 2 * tNorm * tNorm * tNorm;

        double[] qOut = new double[qStart.length];
        for (int i = 0; i < qStart.length; i++) {
            // Direct interpolation without modular wrapping
            double delta = qEnd[i] - qStart[i];
            qOut[i] = qStart[i] + s * delta;
        }
        return qOut;
    }
}