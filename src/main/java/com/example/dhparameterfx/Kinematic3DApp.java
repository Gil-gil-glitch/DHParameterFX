package com.example.dhparameterfx;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Point3D;
import javafx.geometry.Pos;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Cylinder;
import javafx.scene.transform.Affine;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;

public class Kinematic3DApp extends Application {

    private final Group world = new Group();
    private final Group robotGroup = new Group(); // Container specifically for robot geometry
    private final ForwardKinematicsEngine fkEngine = new ForwardKinematicsEngine();

    // Active model parameters
    private final List<DHParameterModel> dhModels = new ArrayList<>();
    private VBox controlsContainer;

    @Override
    public void start(Stage primaryStage) {

        // Set up 3D World
        SubScene subScene = new SubScene(world, 800, 700, true, SceneAntialiasing.BALANCED);
        subScene.setFill(Color.web("#1e1e24"));

        OrbitCamera cameraRig = new OrbitCamera();
        subScene.setCamera(cameraRig.getCamera());

        world.getChildren().add(cameraRig.getRootNode());
        world.getChildren().add(robotGroup);

        // Lighting
        AmbientLight ambient = new AmbientLight(Color.color(0.4, 0.4, 0.4));
        PointLight pointLight = new PointLight(Color.WHITE);
        pointLight.setTranslateX(-20);
        pointLight.setTranslateY(-40);
        pointLight.setTranslateZ(-50);
        world.getChildren().addAll(ambient, pointLight);

        //  Set up Default 3 Joint Robot
        dhModels.add(new DHParameterModel(0.0, 90.0, 5.0, 30.0));   // Joint 1
        dhModels.add(new DHParameterModel(10.0, 0.0, 0.0, -40.0));  // Joint 2
        dhModels.add(new DHParameterModel(8.0, 0.0, 0.0, 25.0));    // Joint 3


        BorderPane root = new BorderPane();
        root.setCenter(subScene);

        subScene.widthProperty().bind(root.widthProperty().subtract(320));
        subScene.heightProperty().bind(root.heightProperty());

        // Side Control Panel
        VBox sidePanel = createControlPanel();
        root.setRight(sidePanel);

        Scene scene = new Scene(root, 1120, 700);
        cameraRig.registerMouseEvents(scene);

        rebuildUIControls();
        updateRobot3D();

        primaryStage.setTitle("JavaFX DH Parameter Calculator - Step 3");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    /**
     * Constructs the UI Control Panel on the right side.
     */
    private VBox createControlPanel() {

        VBox panel = new VBox(10);
        panel.setPrefWidth(320);
        panel.setPadding(new Insets(15));
        panel.setStyle("-fx-background-color: #2b2b36; -fx-text-fill: white;");
        Label header = new Label("Kinematic Chain Setup");
        header.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: white;");

        Button addBtn = new Button("+ Add Joint");
        addBtn.setMaxWidth(Double.MAX_VALUE);
        addBtn.setOnAction(e -> {
            dhModels.add(new DHParameterModel(5.0, 0.0, 0.0, 0.0));
            rebuildUIControls();
            updateRobot3D();
        });

        controlsContainer = new VBox(15);
        ScrollPane scrollPane = new ScrollPane(controlsContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        panel.getChildren().addAll(header, addBtn, new Separator(Orientation.HORIZONTAL), scrollPane);
        return panel;
    }

    /**
     * Rebuilds the control cards for all active joints.
     */
    private void rebuildUIControls() {

        controlsContainer.getChildren().clear();

        for (int i = 0; i < dhModels.size(); i++) {

            int index = i;
            DHParameterModel model = dhModels.get(i);

            VBox card = new VBox(8);
            card.setStyle("-fx-background-color: #3b3b4d; -fx-padding: 10; -fx-background-radius: 5;");

            HBox cardHeader = new HBox();
            cardHeader.setAlignment(Pos.CENTER_LEFT);
            Label title = new Label("Joint " + (i + 1));
            title.setStyle("-fx-font-weight: bold; -fx-text-fill: #61afef;");

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            Button deleteBtn = new Button("X");
            deleteBtn.setStyle("-fx-background-color: #e06c75; -fx-text-fill: white; -fx-font-size: 10px;");
            deleteBtn.setOnAction(e -> {

                if (dhModels.size() > 1) { // Retain at least 1 joint
                    dhModels.remove(index);
                    rebuildUIControls();
                    updateRobot3D();

                }

            });

            cardHeader.getChildren().addAll(title, spacer, deleteBtn);

            // Add Parameter Sliders
            card.getChildren().addAll(
                    cardHeader,
                    createSliderRow("a (Length):", -20, 20, model.aProperty()),
                    createSliderRow("α (Twist °):", -180, 180, model.alphaProperty()),
                    createSliderRow("d (Offset):", -20, 20, model.dProperty()),
                    createSliderRow("θ (Angle °):", -180, 180, model.thetaProperty())

            );

            controlsContainer.getChildren().add(card);
        }
    }

    private HBox createSliderRow(String label, double min, double max, javafx.beans.property.DoubleProperty prop) {

        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_LEFT);
        Label lbl = new Label(label);
        lbl.setPrefWidth(75);
        lbl.setStyle("-fx-text-fill: #abb2bf; -fx-font-size: 11px;");

        Slider slider = new Slider(min, max, prop.get());
        HBox.setHgrow(slider, Priority.ALWAYS);

        Label valueLbl = new Label(String.format("%.1f", prop.get()));
        valueLbl.setPrefWidth(40);
        valueLbl.setStyle("-fx-text-fill: #e5c07b; -fx-font-size: 11px;");

        // Bidirectional listener & auto-update 3D scene
        slider.valueProperty().addListener((obs, oldVal, newVal) -> {
            prop.set(newVal.doubleValue());
            valueLbl.setText(String.format("%.1f", newVal.doubleValue()));
            updateRobot3D();

        });

        row.getChildren().addAll(lbl, slider, valueLbl);
        return row;
    }

    /**
     * Clears and redraws the 3D robot chain based on updated model data.
     */
    private void updateRobot3D() {

        robotGroup.getChildren().clear();

        // Convert UI reactive models to raw DH Parameters
        List<DHParameter> dhParams = new ArrayList<>();
        for (DHParameterModel model : dhModels) {
            dhParams.add(model.toDHParameter());
        }

        List<Matrix4x4> transforms = fkEngine.computeCumulativeTransforms(dhParams);

        for (int i = 0; i < transforms.size(); i++) {
            Matrix4x4 mat = transforms.get(i);

            // Render RGB Axis Frame
            AxisGroup axis = new AxisGroup(3.0, 0.15);
            applyMatrixToNode(axis, mat);
            robotGroup.getChildren().add(axis);

            // Render Link Cylinder
            if (i > 0) {
                double[] pPrev = transforms.get(i - 1).getPosition();
                double[] pCurr = mat.getPosition();

                Point3D start = new Point3D(pPrev[0], pPrev[1], pPrev[2]);
                Point3D end = new Point3D(pCurr[0], pCurr[1], pCurr[2]);

                Node linkCylinder = createLinkCylinder(start, end, 0.4, Color.GRAY);
                robotGroup.getChildren().add(linkCylinder);
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

        if (length < 1e-4) return new Group();

        Point3D mid = p1.add(p2).multiply(0.5);
        Cylinder cylinder = new Cylinder(radius, length);
        cylinder.setMaterial(new PhongMaterial(color));

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