package com.example.dhparameterfx;

import java.util.ArrayList;
import java.util.List;

public class IKSolver {

    private final ForwardKinematicsEngine fkEngine = new ForwardKinematicsEngine();
    private static final double STEP_SIZE = 1e-4;

    /**
     * Estimates maximum kinematic reach from base to end-effector.
     */
    public boolean isTargetReachable(List<DHParameterModel> dhModels, double[] targetPos) {

        if (dhModels.isEmpty()) return false;

        // Compute total link length capacity
        double maxReach = 0;
        for (DHParameterModel m : dhModels) {
            maxReach += Math.abs(m.getA()) + Math.abs(m.getD());

        }

        // Get actual base position from Forward Kinematics
        List<DHParameter> dhParams = getCurrentDHParams(dhModels);
        List<Matrix4x4> transforms = fkEngine.computeCumulativeTransforms(dhParams);

        double[] basePos = new double[]{0, 0, 0};
        if (!transforms.isEmpty()) {

            basePos = transforms.get(0).getPosition();

        }

        double dx = targetPos[0] - basePos[0];
        double dy = targetPos[1] - basePos[1];
        double dz = targetPos[2] - basePos[2];
        double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);


        return dist <= (maxReach * 1.1); // Allow a 10% safety margin for offset alignment
    }

    /**
     * Solves IK iteratively using Damped Least Squares (DLS).
     * Updates dhModels in-place to the best achieved pose.
     *
     * @return true if target was reached within tolerance, false if stopped at closest posture
     */
    public boolean solve(List<DHParameterModel> dhModels, double[] targetPos, int maxIterations, double tolerance) {
        double lambda = 0.15; // Damping constant
        double bestError = Double.MAX_VALUE;
        double[] bestThetas = new double[dhModels.size()];

        for (int iter = 0; iter < maxIterations; iter++) {
            List<DHParameter> dhParams = getCurrentDHParams(dhModels);
            List<Matrix4x4> transforms = fkEngine.computeCumulativeTransforms(dhParams);

            if (transforms.isEmpty()) return false;

            double[] currentPos = transforms.get(transforms.size() - 1).getPosition();

            double ex = targetPos[0] - currentPos[0];
            double ey = targetPos[1] - currentPos[1];
            double ez = targetPos[2] - currentPos[2];

            double errorDist = Math.sqrt(ex * ex + ey * ey + ez * ez);

            // Track best posture achieved
            if (errorDist < bestError) {
                bestError = errorDist;
                for (int i = 0; i < dhModels.size(); i++) {
                    bestThetas[i] = dhModels.get(i).getTheta();
                }
            }

            // Convergence check
            if (errorDist < tolerance) {
                return true;
            }

            int numJoints = dhModels.size();
            double[][] J = computePositionJacobian(dhModels, currentPos);

            // Damped Least Squares: J_damped = J^T * (J * J^T + lambda^2 * I)^-1
            double[][] A = new double[3][3];
            for (int r = 0; r < 3; r++) {
                for (int c = 0; c < 3; c++) {
                    double sum = 0;
                    for (int k = 0; k < numJoints; k++) {
                        sum += J[r][k] * J[c][k];
                    }
                    if (r == c) sum += lambda * lambda;
                    A[r][c] = sum;
                }
            }

            double[][] Ainv = invert3x3(A);
            if (Ainv == null) break;

            double[] dampedErr = new double[3];
            dampedErr[0] = Ainv[0][0] * ex + Ainv[0][1] * ey + Ainv[0][2] * ez;
            dampedErr[1] = Ainv[1][0] * ex + Ainv[1][1] * ey + Ainv[1][2] * ez;
            dampedErr[2] = Ainv[2][0] * ex + Ainv[2][1] * ey + Ainv[2][2] * ez;

            for (int j = 0; j < numJoints; j++) {
                double dq = J[0][j] * dampedErr[0] + J[1][j] * dampedErr[1] + J[2][j] * dampedErr[2];
                double newTheta = dhModels.get(j).getTheta() + Math.toDegrees(dq);

                // Angle wrapping [-180, 180]
                while (newTheta > 180) newTheta -= 360;
                while (newTheta < -180) newTheta += 360;

                dhModels.get(j).setTheta(newTheta);
            }
        }

        // If exact tolerance wasn't met, restore best configuration achieved
        for (int i = 0; i < dhModels.size(); i++) {
            dhModels.get(i).setTheta(bestThetas[i]);
        }

        // Return true if close enough (within 0.5 units) to proceed with animation
        return bestError <= 0.5;
    }

    private double[][] computePositionJacobian(List<DHParameterModel> dhModels, double[] currentPos) {
        int n = dhModels.size();
        double[][] J = new double[3][n];

        for (int j = 0; j < n; j++) {
            DHParameterModel model = dhModels.get(j);
            double origTheta = model.getTheta();

            model.setTheta(origTheta + Math.toDegrees(STEP_SIZE));
            List<Matrix4x4> transformsP = fkEngine.computeCumulativeTransforms(getCurrentDHParams(dhModels));
            double[] posP = transformsP.get(transformsP.size() - 1).getPosition();

            model.setTheta(origTheta);

            J[0][j] = (posP[0] - currentPos[0]) / STEP_SIZE;
            J[1][j] = (posP[1] - currentPos[1]) / STEP_SIZE;
            J[2][j] = (posP[2] - currentPos[2]) / STEP_SIZE;
        }

        return J;
    }

    private List<DHParameter> getCurrentDHParams(List<DHParameterModel> dhModels) {
        List<DHParameter> list = new ArrayList<>();
        for (DHParameterModel m : dhModels) list.add(m.toDHParameter());
        return list;
    }

    private double[][] invert3x3(double[][] m) {
        double det = m[0][0] * (m[1][1] * m[2][2] - m[1][2] * m[2][1])
                - m[0][1] * (m[1][0] * m[2][2] - m[1][2] * m[2][0])
                + m[0][2] * (m[1][0] * m[2][1] - m[1][1] * m[2][0]);

        if (Math.abs(det) < 1e-9) return null;

        double invdet = 1.0 / det;
        double[][] inv = new double[3][3];

        inv[0][0] = (m[1][1] * m[2][2] - m[1][2] * m[2][1]) * invdet;
        inv[0][1] = (m[0][2] * m[2][1] - m[0][1] * m[2][2]) * invdet;
        inv[0][2] = (m[0][1] * m[1][2] - m[0][2] * m[1][1]) * invdet;
        inv[1][0] = (m[1][2] * m[2][0] - m[1][0] * m[2][2]) * invdet;
        inv[1][1] = (m[0][0] * m[2][2] - m[0][2] * m[2][0]) * invdet;
        inv[1][2] = (m[0][2] * m[1][0] - m[0][0] * m[1][2]) * invdet;
        inv[2][0] = (m[1][0] * m[2][1] - m[1][1] * m[2][0]) * invdet;
        inv[2][1] = (m[0][1] * m[1][0] - m[0][0] * m[2][1]) * invdet;
        inv[2][2] = (m[0][0] * m[1][1] - m[0][1] * m[1][0]) * invdet;

        return inv;
    }
}