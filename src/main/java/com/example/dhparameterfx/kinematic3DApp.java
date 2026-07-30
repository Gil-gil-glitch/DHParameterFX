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
import javafx.scene.shape.Box;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.Cylinder;
import javafx.scene.shape.DrawMode;
import javafx.scene.transform.Affine;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;

public class kinematic3DApp extends Application {

    private final Group world = new Group();
    private final Group robotGroup = new Group();
    private final ForwardKinematicsEngine fkEngine = new ForwardKinematicsEngine();

    private final List<DHParameterModel> dhModels = new ArrayList<>();
    private VBox controlsContainer;
    private MatrixDisplayHUD hud;

    private int selectedJointIndex = 0;

    private Node createSelectionHighlightBox(double size) {
        Box box = new Box(size, size, size);
        box.setDrawMode(DrawMode.LINE);
        box.setCullFace(CullFace.NONE);

        PhongMaterial blueWireframe = new PhongMaterial(Color.web("#61afef"));
        box.setMaterial(blueWireframe);

        return box;
    }

    @Override
    public void start(Stage primaryStage) {

        SubScene subScene = new SubScene(world, 800, 700, true, SceneAntialiasing.BALANCED);
        subScene.setFill(Color.web("#1e1e24"));

        OrbitCamera cameraRig = new OrbitCamera();
        subScene.setCamera(cameraRig.getCamera());

        world.getChildren().add(cameraRig.getRootNode());
        robotGroup.getTransforms().add(new Rotate(-270, Rotate.X_AXIS));
        robotGroup.getTransforms().add(new Rotate(270, Rotate.Z_AXIS));
        world.getChildren().add(robotGroup);

        // Lighting
        AmbientLight ambient = new AmbientLight(Color.color(0.4, 0.4, 0.4));
        PointLight pointLight = new PointLight(Color.WHITE);
        pointLight.setTranslateX(-20);
        pointLight.setTranslateY(-40);
        pointLight.setTranslateZ(-50);
        world.getChildren().addAll(ambient, pointLight);

        // Default 3 Joint Robot
        dhModels.add(new DHParameterModel(0.0, 90.0, 5.0, 30.0));
        dhModels.add(new DHParameterModel(10.0, 0.0, 0.0, 40.0));
        dhModels.add(new DHParameterModel(8.0, 0.0, 0.0, -25.0));

        // Create HUD Overlay
        hud = new MatrixDisplayHUD();

        // Layer SubScene and HUD using StackPane
        StackPane viewportPane = new StackPane();
        viewportPane.getChildren().addAll(subScene, hud);
        StackPane.setAlignment(hud, Pos.TOP_LEFT);
        StackPane.setMargin(hud, new Insets(15));

        BorderPane root = new BorderPane();
        root.setCenter(viewportPane);

        subScene.widthProperty().bind(viewportPane.widthProperty());
        subScene.heightProperty().bind(viewportPane.heightProperty());

        VBox sidePanel = createControlPanel();
        root.setRight(sidePanel);

        Scene scene = new Scene(root, 1120, 700);

        subScene.setOnMouseClicked(event -> {
            Node picked = event.getPickResult().getIntersectedNode();
            Node current = picked;
            while (current != null && !(current instanceof AxisGroup) && current != robotGroup) {
                current = current.getParent();
            }

            if (current instanceof AxisGroup axis) {
                Object tag = axis.getUserData();

                if (tag instanceof Integer jointIdx) {
                    selectedJointIndex = jointIdx;
                    rebuildUIControls();
                    updateRobot3D();

                }

            }
        });

        cameraRig.registerMouseEvents(scene);

        rebuildUIControls();
        updateRobot3D();

        primaryStage.setTitle("JavaFX DH Parameter Calculator");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private VBox createControlPanel() {
        VBox panel = new VBox(10);
        panel.setPrefWidth(320);
        panel.setPadding(new Insets(15));
        panel.setStyle("-fx-background-color: #2b2b36; -fx-text-fill: white;");

        Label header = new Label("Kinematic Chain Setup");
        header.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: white;");

        // Add Joint Button
        Button addBtn = new Button("+ Add Joint");
        addBtn.setMaxWidth(Double.MAX_VALUE);
        addBtn.setStyle("-fx-background-color: #98c379; -fx-text-fill: #1e1e24; -fx-font-weight: bold;");
        addBtn.setOnAction(e -> {
            dhModels.add(new DHParameterModel(5.0, 0.0, 0.0, 0.0));
            selectedJointIndex = dhModels.size() - 1;
            rebuildUIControls();
            updateRobot3D();
        });

        // Preset Selection Toolbar
        Label presetsLabel = new Label("Presets:");
        presetsLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #abb2bf; -fx-font-weight: bold;");

        HBox presetBar = new HBox(8);
        Button scaraBtn = new Button("SCARA");
        scaraBtn.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(scaraBtn, Priority.ALWAYS);
        scaraBtn.setStyle("-fx-background-color: #3b3b4d; -fx-text-fill: #61afef; -fx-border-color: #61afef; -fx-border-radius: 3;");
        scaraBtn.setOnAction(e -> loadScaraPreset());

        Button pumaBtn = new Button("PUMA 560");
        pumaBtn.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(pumaBtn, Priority.ALWAYS);
        pumaBtn.setStyle("-fx-background-color: #3b3b4d; -fx-text-fill: #e5c07b; -fx-border-color: #e5c07b; -fx-border-radius: 3;");
        pumaBtn.setOnAction(e -> loadPuma560Preset());

        presetBar.getChildren().addAll(scaraBtn, pumaBtn);

        controlsContainer = new VBox(15);
        ScrollPane scrollPane = new ScrollPane(controlsContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        panel.getChildren().addAll(
                header,
                addBtn,
                presetsLabel,
                presetBar,
                new Separator(Orientation.HORIZONTAL),
                scrollPane
        );

        return panel;
    }

    private void rebuildUIControls() {
        controlsContainer.getChildren().clear();

        for (int i = 0; i < dhModels.size(); i++) {

            int index = i;
            DHParameterModel model = dhModels.get(i);

            VBox card = new VBox(8);

            boolean isSelected = (i == selectedJointIndex);
            String borderStyle = isSelected ? "-fx-border-color: #61afef; -fx-border-width: 2px; -fx-border-radius: 5;" : "";
            String bgStyle = isSelected ? "-fx-background-color: #2c313a;" : "-fx-background-color: #3b3b4d;";

            card.setStyle(STR."\{bgStyle} -fx-padding: 10; -fx-background-radius: 5; \{borderStyle}");

            card.setOnMouseClicked(e -> {
                selectedJointIndex = index;
                rebuildUIControls();
                updateRobot3D();
            });

            HBox cardHeader = new HBox();
            cardHeader.setAlignment(Pos.CENTER_LEFT);
            Label title = new Label(STR."Joint \{i + 1}\{isSelected ? " (Selected)" : ""}");
            title.setStyle(STR."-fx-font-weight: bold; -fx-text-fill: \{isSelected ? "#61afef" : "#abb2bf"};");

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            Button deleteBtn = new Button("X");
            deleteBtn.setStyle("-fx-background-color: #e06c75; -fx-text-fill: white; -fx-font-size: 10px;");
            deleteBtn.setOnAction(e -> {
                if (dhModels.size() > 1) {
                    dhModels.remove(index);
                    if (selectedJointIndex >= dhModels.size()) {
                        selectedJointIndex = dhModels.size() - 1;
                    }
                    rebuildUIControls();
                    updateRobot3D();
                }
            });

            cardHeader.getChildren().addAll(title, spacer, deleteBtn);

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

        slider.valueProperty().addListener((obs, oldVal, newVal) -> {
            prop.set(newVal.doubleValue());
            valueLbl.setText(String.format("%.1f", newVal.doubleValue()));
            updateRobot3D();
        });

        row.getChildren().addAll(lbl, slider, valueLbl);
        return row;
    }

    private void updateRobot3D() {
        robotGroup.getChildren().clear();

        List<DHParameter> dhParams = new ArrayList<>();
        for (DHParameterModel model : dhModels) {
            dhParams.add(model.toDHParameter());
        }

        List<Matrix4x4> transforms = fkEngine.computeCumulativeTransforms(dhParams);

        // Update HUD with cumulative transform of end-effector (last matrix)
        if (!transforms.isEmpty() && hud != null) {
            hud.update(transforms.get(transforms.size() - 1));
        }

        for (int i = 0; i < transforms.size(); i++) {
            Matrix4x4 mat = transforms.get(i);

            AxisGroup axis = new AxisGroup(3.0, 0.2, i);
            axis.setUserData(i);
            applyMatrixToNode(axis, mat);
            robotGroup.getChildren().add(axis);

            if (i == selectedJointIndex) {
                Node highlightBox = createSelectionHighlightBox(4.5);
                applyMatrixToNode(highlightBox, mat);
                robotGroup.getChildren().add(highlightBox);

            }

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

    /**
     * Loads a standard 3-DOF SCARA robot geometry into the model list.
     */
    private void loadScaraPreset() {
        dhModels.clear();
        // Joint 1: Base rotation
        dhModels.add(new DHParameterModel(10.0, 0.0, 0.0, 0.0));
        // Joint 2: Elbow rotation
        dhModels.add(new DHParameterModel(8.0, 180.0, 0.0, 0.0));
        // Joint 3: Wrist translation / offset
        dhModels.add(new DHParameterModel(0.0, 0.0, 5.0, 0.0));

        selectedJointIndex = 0;
        rebuildUIControls();
        updateRobot3D();
    }

    /**
     * Loads the standard 6-DOF PUMA 560 robot geometry into the model list.
     */
    private void loadPuma560Preset() {
        dhModels.clear();
        // Joint 1: Waist rotation
        dhModels.add(new DHParameterModel(0.0, -90.0, 0.0, 0.0));
        // Joint 2: Shoulder pitch
        dhModels.add(new DHParameterModel(8.0, 0.0, 0.0, -30.0));
        // Joint 3: Elbow pitch
        dhModels.add(new DHParameterModel(2.0, 90.0, 0.0, 45.0));
        // Joint 4: Wrist roll
        dhModels.add(new DHParameterModel(0.0, -90.0, 8.0, 0.0));
        // Joint 5: Wrist pitch
        dhModels.add(new DHParameterModel(0.0, 90.0, 0.0, 30.0));
        // Joint 6: Tool roll
        dhModels.add(new DHParameterModel(0.0, 0.0, 2.0, 0.0));

        selectedJointIndex = 0;
        rebuildUIControls();
        updateRobot3D();
    }

    public static void main(String[] args) {
        launch(args);
    }
}