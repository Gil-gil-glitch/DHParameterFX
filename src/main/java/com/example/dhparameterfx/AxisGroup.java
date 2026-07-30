package com.example.dhparameterfx;

import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Cylinder;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;

public class AxisGroup extends Group {

    public AxisGroup(double axisLength, double radius) {

        // X Axis (Red)
        Cylinder xAxis = createAxisCylinder(axisLength, radius, Color.RED);
        xAxis.getTransforms().addAll(new Translate(axisLength / 2, 0, 0), new Rotate(90, Rotate.Z_AXIS));

        // Y Axis (Green)
        Cylinder yAxis = createAxisCylinder(axisLength, radius, Color.GREEN);
        yAxis.getTransforms().add(new Translate(0, axisLength / 2, 0));

        // Z Axis (Blue)
        Cylinder zAxis = createAxisCylinder(axisLength, radius, Color.BLUE);
        zAxis.getTransforms().addAll(new Translate(0, 0, axisLength / 2), new Rotate(90, Rotate.X_AXIS));

        getChildren().addAll(xAxis, yAxis, zAxis);

    }

    private Cylinder createAxisCylinder(double length, double radius, Color color) {
        Cylinder cyl = new Cylinder(radius, length);
        cyl.setMaterial(new PhongMaterial(color));
        return cyl;

    }

}