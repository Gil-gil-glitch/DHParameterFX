package com.example.dhparameterfx;

import javafx.scene.Group;
import javafx.scene.PerspectiveCamera;
import javafx.scene.Scene;
import javafx.scene.input.MouseButton;
import javafx.scene.input.ScrollEvent;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;

public class OrbitCamera {

    private final PerspectiveCamera camera = new PerspectiveCamera(true);
    private final Group cameraXform = new Group();
    private final Group cameraXform2 = new Group();
    private final Rotate rotateX = new Rotate(0, Rotate.X_AXIS);
    private final Rotate rotateY = new Rotate(0, Rotate.Y_AXIS);
    private final Translate translate = new Translate(0, 0, -50); // Start offset

    private double mouseX, mouseY;

    public OrbitCamera() {
        camera.setNearClip(0.1);
        camera.setFarClip(1000.0);

        // Assemble camera hierarchy
        cameraXform.getTransforms().addAll(rotateY, rotateX);
        cameraXform2.getChildren().add(camera);
        cameraXform2.getTransforms().add(translate);
        cameraXform.getChildren().add(cameraXform2);


        rotateX.setAngle(-30); // Default orientation
        rotateY.setAngle(45);

    }

    public PerspectiveCamera getCamera() {
        return camera;

    }

    public Group getRootNode() {
        return cameraXform;

    }

    public void registerMouseEvents(Scene scene) {

        scene.setOnMousePressed(event -> {
            mouseX = event.getSceneX();
            mouseY = event.getSceneY();

        });

        scene.setOnMouseDragged(event -> {
            double deltaX = event.getSceneX() - mouseX;
            double deltaY = event.getSceneY() - mouseY;

            if (event.getButton() == MouseButton.SECONDARY) { // Orbit

                rotateY.setAngle(rotateY.getAngle() + deltaX * 0.3);
                rotateX.setAngle(rotateX.getAngle() - deltaY * 0.3);

            }

            else if (event.getButton() == MouseButton.MIDDLE) { // Pan

                cameraXform.setTranslateX(cameraXform.getTranslateX() + deltaX * 0.1);
                cameraXform.setTranslateY(cameraXform.getTranslateY() + deltaY * 0.1);

            }

            mouseX = event.getSceneX();
            mouseY = event.getSceneY();

        });

        scene.setOnScroll((ScrollEvent event) -> { // Zoom
            double z = translate.getZ() + event.getDeltaY() * 0.2;
            translate.setZ(Math.min(-5, Math.max(-200, z))); // Clamp zoom
        });
    }
}