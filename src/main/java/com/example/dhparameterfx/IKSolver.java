package com.example.dhparameterfx;

import java.util.ArrayList;
import java.util.List;

public class IKSolver {

    private final ForwardKinematicsEngine fkEngine = new ForwardKinematicsEngine();
    private static final double STEP_SIZE = 1e-4;

    /**
     * Checks if target is within maximum possible arm length.
     */
    public boolean isTargetReachable(List<DHParameterModel> dhModels, double[] targetPos) {

        double maxReach = 0;

        for (DHParameterModel m : dhModels) {

            maxReach += Math.abs(m.getA()) + Math.abs(m.getD());

        }

        // Base location is origin (0,0,0) in robot frame

        double dist = Math.sqrt(targetPos[0] * targetPos[0] +  targetPos[1] * targetPos[1] + targetPos[2] * targetPos[2]);

        return dist <= maxReach;

    }

    public boolean solve(List<DHParameterModel> dhModels, double[] targetPos, int maxIterations, double tolerance) {

        double lambda = 0.2; // Damping parameter for numerical stability near singularities

        for (int iter = 0; iter < maxIterations; iter++) {

            List<DHParameter> dhParams = getCurrentDHParams(dhModels);
            List<Matrix4x4> transforms = fkEngine.computeCumulativeTransforms(dhParams);

            if (transforms.isEmpty()) return false;

            double[] currentPos = transforms.get(transforms.size() - 1).getPosition();

            double ex = targetPos[0] - currentPos[0];
            double ey = targetPos[1] - currentPos[1];
            double ez = targetPos[2] - currentPos[2];

            double errorDist = Math.sqrt(ex * ex + ey * ey + ez * ez);

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

                // Set joint limits to [-180, 180] degrees
                if (newTheta > 180) newTheta = 180;
                if (newTheta < -180) newTheta = -180;

                dhModels.get(j).setTheta(newTheta);

            }

        }

        return false;

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