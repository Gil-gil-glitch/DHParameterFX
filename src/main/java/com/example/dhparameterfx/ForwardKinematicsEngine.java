package com.example.dhparameterfx;

import java.util.ArrayList;
import java.util.List;

public class ForwardKinematicsEngine {

    /**
     * Computes cumulative transformation matrices for each joint relative to the base frame (0).
     *
     * @param parameters List of DH parameters for links 1..N
     * @return List of 4x4 matrices where index i is T_0_to_i (index 0 = base frame identity)
     */

    public List<Matrix4x4> computeCumulativeTransforms(List<DHParameter> parameters) {

        List<Matrix4x4> transforms = new ArrayList<>();

        // Base frame T0
        Matrix4x4 currentCumulative = Matrix4x4.identity();
        transforms.add(currentCumulative);

        for (DHParameter param : parameters) {

            Matrix4x4 localTransform = param.toTransformMatrix();
            currentCumulative = currentCumulative.multiply(localTransform);
            transforms.add(currentCumulative);

        }

        return transforms;
    }

    /**
     * Utility to extract just the 3D position points for each frame origin.
     */
    public List<double[]> getJointPositions(List<DHParameter> parameters) {

        List<Matrix4x4> transforms = computeCumulativeTransforms(parameters);

        List<double[]> positions = new ArrayList<>();

        for (Matrix4x4 transform : transforms) {

            positions.add(transform.getPosition());

        }
        return positions;
    }
}