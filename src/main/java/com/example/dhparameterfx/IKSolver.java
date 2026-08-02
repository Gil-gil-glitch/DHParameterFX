package com.example.dhparameterfx;

import java.util.ArrayList;
import java.util.List;

public class IKSolver {

    private final ForwardKinematicsEngine fkEngine = new ForwardKinematicsEngine();
    private static final double STEP_SIZE = 1e-4; // Step size for numerical differentiation

    /**
     * Solves IK iteratively to move end-effector closer to targetPos.
     * Updates theta values in dhModels in-place.
     *
     * @return true if converged within tolerance
     */

    public boolean solve(List<DHParameterModel> dhModels, double[] targetPos, double maxIterations, double tolerance) {

        double lambda = 0.1; // Damping factor

        for (int iter = 0; iter < maxIterations; iter++) {

            List<DHParameter> dhParams = getCurrentDHParams(dhModels);
            List<Matrix4x4> transforms = fkEngine.computeCumulativeTransforms(dhParams);

            if (transforms.isEmpty()) return false;

            double[] currentPos = transforms.get(transforms.size() - 1).getPosition();

            // Error vector e = target - current
            double ex = targetPos[0] - currentPos[0]; // x
            double ey = targetPos[1] - currentPos[1]; // y
            double ez = targetPos[2] - currentPos[2]; // z

            double errorDist = Math.sqrt(ex * ex + ey * ey + ez * ez);

            if (errorDist < tolerance) {

                return true; // Converged

            }

            int numJoints = dhModels.size();
            double[][] J = computePositionJacobian(dhModels, currentPos);

            // Compute Damped Pseudo-Inverse: J_damped = J^T * (J * J^T + lambda^2 * I)^-1
            // For a 3xN Jacobian: A = J * J^T is a 3x3 matrix
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

            // Invert 3x3 matrix A
            double[][] Ainv = invert3x3(A);
            if (Ainv == null) break;

            // Compute Ainv * e
            double[] dampedErr = new double[3];
            dampedErr[0] = Ainv[0][0] * ex + Ainv[0][1] * ey + Ainv[0][2] * ez;
            dampedErr[1] = Ainv[1][0] * ex + Ainv[1][1] * ey + Ainv[1][2] * ez;
            dampedErr[2] = Ainv[2][0] * ex + Ainv[2][1] * ey + Ainv[2][2] * ez;

            // delta_q = J^T * dampedErr
            for (int j = 0; j < numJoints; j++) {
                double dq = J[0][j] * dampedErr[0] + J[1][j] * dampedErr[1] + J[2][j] * dampedErr[2];
                double newTheta = dhModels.get(j).getTheta() + Math.toDegrees(dq);

                // Wrap angles between -180 and 180
                while (newTheta > 180) newTheta -= 360;
                while (newTheta < -180) newTheta += 360;

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

            // Finite difference perturbation
            model.setTheta(origTheta + Math.toDegrees(STEP_SIZE));
            List<Matrix4x4> transformsP = fkEngine.computeCumulativeTransforms(getCurrentDHParams(dhModels));
            double[] posP = transformsP.get(transformsP.size() - 1).getPosition();

            model.setTheta(origTheta); // Reset

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