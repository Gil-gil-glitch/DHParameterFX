package com.example.dhparameterfx;

import com.example.dhparameterfx.GizmoData;
import com.example.dhparameterfx.GizmoType;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Cylinder;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;

public class AxisGroup extends Group {

    // UPDATE: Now takes jointIndex so we know which joint to modify
    public AxisGroup(double axisLength, double radius, int jointIndex) {

        // Joint Motor (Gold)
        double jointHeight = 2.0;
        Cylinder jointHub = new Cylinder(radius * 5, jointHeight);
        jointHub.setMaterial(new PhongMaterial(Color.web("#e5c07b")));
        jointHub.getTransforms().add(new Rotate(90, Rotate.X_AXIS));
        jointHub.setUserData(new GizmoData(jointIndex, GizmoType.ROTATE_THETA));

        // X Axis (Red)
        Cylinder xAxis = createAxisCylinder(axisLength, radius, Color.RED);
        xAxis.getTransforms().addAll(new Translate(axisLength / 2, 0, 0), new Rotate(90, Rotate.Z_AXIS));
        xAxis.setUserData(new GizmoData(jointIndex, GizmoType.DRAG_A));

        // Y Axis (Green)
        Cylinder yAxis = createAxisCylinder(axisLength, radius, Color.GREEN);
        yAxis.getTransforms().add(new Translate(0, axisLength / 2, 0));
        yAxis.setUserData(new GizmoData(jointIndex, GizmoType.ROTATE_ALPHA));

        // Z Axis (Blue)
        Cylinder zAxis = createAxisCylinder(axisLength, radius, Color.BLUE);
        zAxis.getTransforms().addAll(new Translate(0, 0, axisLength / 2), new Rotate(90, Rotate.X_AXIS));
        zAxis.setUserData(new GizmoData(jointIndex, GizmoType.DRAG_D));

        getChildren().addAll(jointHub, xAxis, yAxis, zAxis);

    }

    private Cylinder createAxisCylinder(double length, double radius, Color color) {

        Cylinder cyl = new Cylinder(radius, length);
        cyl.setMaterial(new PhongMaterial(color));
        return cyl;

    }
}