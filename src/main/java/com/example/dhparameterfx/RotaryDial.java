package com.example.dhparameterfx;


import javafx.beans.property.DoubleProperty;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;

public class RotaryDial extends StackPane {

    public RotaryDial(DoubleProperty angleProperty, double minAngle, double maxAngle) {
        Circle background = new Circle(20, Color.web("#1e1e24"));
        background.setStroke(Color.web("#4b5263"));
        background.setStrokeWidth(2);

        Line indicator = new Line(0, 0, 0, -15);
        indicator.setStroke(Color.web("#98c379"));
        indicator.setStrokeWidth(3);

        this.getChildren().addAll(background, indicator);

        // Bind initial rotation
        indicator.setRotate(angleProperty.get());
        angleProperty.addListener((obs, oldVal, newVal) -> indicator.setRotate(newVal.doubleValue()));

        // Handle Mouse Dragging for circular motion
        this.setOnMouseDragged(e -> {

            // Calculate angle based on mouse position relative to center of the dial
            double dx = e.getX() - 20;
            double dy = e.getY() - 20;

            double rad = Math.atan2(dy, dx);
            double deg = Math.toDegrees(rad) + 90; // Shift so top is 0 degrees

            // Normalize to -180 to 180
            if (deg > 180) deg -= 360;
            if (deg < -180) deg += 360;

            // Clamp to limits
            if (deg > maxAngle) deg = maxAngle;
            if (deg < minAngle) deg = minAngle;

            angleProperty.set(deg);

        });

        this.setStyle("-fx-cursor: hand;");

    }
}