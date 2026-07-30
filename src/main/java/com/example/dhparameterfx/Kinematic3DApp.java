package com.example.dhparameterfx;

import javafx.application.Application;
import javafx.geometry.Point3D;
import javafx.scene.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Cylinder;
import javafx.scene.transform.Affine;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;
import javafx.stage.Stage;

import java.util.List;

public class Kinematic3DApp extends Application {

    private final Group world = new Group();
    private final ForwardKinematicsEngine fkEngine = new ForwardKinematicsEngine();

    @Override
    public void start(Stage primaryStage) {

        // Setup 3D Scene Container
        SubScene subScene = new SubScene(world, 1024, 768, true, SceneAntialiasing.BALANCED);
        subScene.setFill(Color.web("#1e1e24"));

        OrbitCamera cameraRig = new OrbitCamera();
        subScene.setCamera(cameraRig.getCamera());
        world.getChildren().add(cameraRig.getRootNode());

        // Add Ambient / Directional Lighting
        AmbientLight ambient = new AmbientLight(Color.color(0.4, 0.4, 0.4));
        PointLight pointLight = new PointLight(Color.WHITE);
        pointLight.setTranslateX(-20);
        pointLight.setTranslateY(-40);
        pointLight.setTranslateZ(-50);
        world.getChildren().addAll(ambient, pointLight);

        // Define a 3-DOF Kinematic Arm Parameters
        List<DHParameter> armParams = List.of(
                new DHParameter(0,  Math.toRadians(90), 5.0, Math.toRadians(30)),  // Joint 1
                new DHParameter(10, 0,                  0.0, Math.toRadians(-40)), // Joint 2
                new DHParameter(8,  0,                  0.0, Math.toRadians(25))   // Joint 3
        );

        // Render the robot frames and links
        renderRobot(armParams);

        // Main JavaFX Scene Setup
        Group root = new Group(subScene);
        Scene scene = new Scene(root, 1024, 768);
        cameraRig.registerMouseEvents(scene);

        primaryStage.setTitle("JavaFX DH Parameter Visualizer - Step 2");
        primaryStage.setScene(scene);
        primaryStage.show();

    }

    private void renderRobot(List<DHParameter> dhParams) {

        List<Matrix4x4> transforms = fkEngine.computeCumulativeTransforms(dhParams);

        for (int i = 0; i < transforms.size(); i++) {

            Matrix4x4 mat = transforms.get(i);

            // Render Axis Frame at current transformation
            AxisGroup axis = new AxisGroup(3.0, 0.15);
            applyMatrixToNode(axis, mat);
            world.getChildren().add(axis);

            // Render Link Cylinder connecting O_{i-1} to O_i
            if (i > 0) {
                double[] pPrev = transforms.get(i - 1).getPosition();
                double[] pCurr = mat.getPosition();

                Point3D start = new Point3D(pPrev[0], pPrev[1], pPrev[2]);
                Point3D end = new Point3D(pCurr[0], pCurr[1], pCurr[2]);

                Node linkCylinder = createLinkCylinder(start, end, 0.4, Color.GRAY);
                world.getChildren().add(linkCylinder);

            }

        }

    }

    private void applyMatrixToNode(Node node, Matrix4x4 m) {

        Affine affine = new Affine(
                m.get(0, 0), m.get(0, 1), m.get(0, 2), m.get(0, 3),
                m.get(1, 0), m.get(1, 1), m.get(1, 2), m.get(1, 3),
                m.get(2, 0), m.get(2, 1), m.get(2, 2), m.get(2, 3)

        );
        node.getTransforms().add(affine);

    }

    private Node createLinkCylinder(Point3D p1, Point3D p2, double radius, Color color) {

        Point3D diff = p2.subtract(p1);
        double length = diff.magnitude();

        if (length < 1e-4) return new Group(); // Skip zero-length links

        Point3D mid = p1.add(p2).multiply(0.5);
        Cylinder cylinder = new Cylinder(radius, length);
        cylinder.setMaterial(new PhongMaterial(color));

        // Orient cylinder along diff vector
        Point3D yAxis = new Point3D(0, 1, 0);
        Point3D axisOfRot = yAxis.crossProduct(diff);
        double angle = yAxis.angle(diff);

        cylinder.getTransforms().add(new Translate(mid.getX(), mid.getY(), mid.getZ()));

        if (axisOfRot.magnitude() > 1e-4) {
            cylinder.getTransforms().add(new Rotate(angle, axisOfRot));

        }

        return cylinder;

    }

    public static void main(String[] args) {
        launch(args);

    }
}