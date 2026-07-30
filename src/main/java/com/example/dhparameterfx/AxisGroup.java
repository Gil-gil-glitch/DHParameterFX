package com.example.dhparameterfx;

import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Cylinder;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;



public class AxisGroup extends Group {

    public AxisGroup(double axisLength, double radius) {

        // Joint Motor Visualizer (Cylinder aligned along Z-axis)
        double jointHeight = 2.0;
        double jointRadius = 0.8;
        Cylinder jointHub = new Cylinder(jointRadius, jointHeight);
        jointHub.setMaterial(new PhongMaterial(Color.web("#e5c07b"))); // Gold/Brass color for joint

        jointHub.getTransforms().add(new Rotate(90, Rotate.X_AXIS)); // In JavaFX, Cylinders default to Y-axis, so rotate 90 deg around X to align with Z-axis

        // Coordinate Axes
        // X Axis (Red)
        Cylinder xAxis = createAxisCylinder(axisLength, radius, Color.RED);
        xAxis.getTransforms().addAll(new Translate(axisLength / 2, 0, 0), new Rotate(90, Rotate.Z_AXIS));

        // Y Axis (Green)
        Cylinder yAxis = createAxisCylinder(axisLength, radius, Color.GREEN);
        yAxis.getTransforms().add(new Translate(0, axisLength / 2, 0));

        // Z Axis (Blue) - Joint Axis of Rotation
        Cylinder zAxis = createAxisCylinder(axisLength, radius, Color.BLUE);
        zAxis.getTransforms().addAll(new Translate(0, 0, axisLength / 2), new Rotate(90, Rotate.X_AXIS));

        getChildren().addAll(jointHub, xAxis, yAxis, zAxis);
    }

    private Cylinder createAxisCylinder(double length, double radius, Color color) {
        Cylinder cyl = new Cylinder(radius, length);
        cyl.setMaterial(new PhongMaterial(color));
        return cyl;
    }
}