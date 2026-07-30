package com.example.dhparameterfx;

import java.util.List;

public class Main {
    public static void main(String[] args) {
        ForwardKinematicsEngine fk = new ForwardKinematicsEngine();

        List<DHParameter> robot = List.of(
                new DHParameter(10.0, 0.0, 0.0, Math.toRadians(90)), // Joint 1
                new DHParameter(5.0,  0.0, 0.0, Math.toRadians(0))   // Joint 2
        );

        List<Matrix4x4> frames = fk.computeCumulativeTransforms(robot);

        for (int i = 0; i < frames.size(); i++) {
            double[] pos = frames.get(i).getPosition();
            System.out.printf("Frame %d Position: (%.2f, %.2f, %.2f)\n", i, pos[0], pos[1], pos[2]);
        }
    }
}