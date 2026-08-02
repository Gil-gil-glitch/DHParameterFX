package com.example.dhparameterfx;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.beans.property.DoubleProperty;
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
import javafx.scene.shape.Sphere;
import javafx.scene.transform.Affine;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class kinematic3DApp extends Application {

    private final Group world = new Group();
    private final Group robotGroup = new Group();
    private final ForwardKinematicsEngine fkEngine = new ForwardKinematicsEngine();
    private final IKSolver ikSolver = new IKSolver();

    private final List<DHParameterModel> dhModels = new ArrayList<>();
    private VBox controlsContainer;
    private MatrixDisplayHUD hud;

    private int selectedJointIndex = 0;

    // IK & Trajectory state
    private final Sphere targetSphere = new Sphere(1.2);
    private final double[] targetPos = new double[]{10.0, 5.0, 5.0};
    private AnimationTimer playbackTimer;

    private Node createSelectionHighlightBox(double size) {
        Box box = new Box(size, size, size);
        box.setDrawMode(DrawMode.LINE);
        box.setCullFace(CullFace.NONE);
        box.setMaterial(new PhongMaterial(Color.web("#61afef")));
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

        // Target Sphere
        targetSphere.setMaterial(new PhongMaterial(Color.web("#e06c75")));
        robotGroup.getChildren().add(targetSphere);
        updateTargetSpherePosition();

        // Lighting
        AmbientLight ambient = new AmbientLight(Color.color(0.4, 0.4, 0.4));
        PointLight pointLight = new PointLight(Color.WHITE);
        pointLight.setTranslateX(-20);
        pointLight.setTranslateY(-40);
        pointLight.setTranslateZ(-50);
        world.getChildren().addAll(ambient, pointLight);

        // Default 3-Joint Robot
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

        VBox sidePanel = createControlPanel(primaryStage);
        root.setRight(sidePanel);

        Scene scene = new Scene(root, 1150, 750);

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

        primaryStage.setTitle("JavaFX DH Parameter & Kinematics Workspace");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void updateTargetSpherePosition() {
        targetSphere.setTranslateX(targetPos[0]);
        targetSphere.setTranslateY(targetPos[1]);
        targetSphere.setTranslateZ(targetPos[2]);
    }

    private void runIKAndAnimate() {
        // 1. Check basic workspace reachability
        if (!ikSolver.isTargetReachable(dhModels, targetPos)) {
            showWarningDialog("Unreachable Target",
                    "The target position (" + String.format("%.1f, %.1f, %.1f", targetPos[0], targetPos[1], targetPos[2]) +
                            ") is beyond the robot's kinematic reach.");
            return;
        }

        // 2. Capture starting configuration
        double[] qStart = new double[dhModels.size()];
        for (int i = 0; i < dhModels.size(); i++) {
            qStart[i] = dhModels.get(i).getTheta();
        }

        // 3. Solve IK (350 iterations, 0.1 tolerance)
        boolean solved = ikSolver.solve(dhModels, targetPos, 350, 0.1);

        if (!solved) {
            // If best error distance > 0.5, alert user
            showWarningDialog("Target Unreachable",
                    "The manipulator reached its limit towards the target, but could not match the exact position due to joint constraints.");
            return;
        }

        // 4. Record target joint configuration & reset to start position for trajectory
        double[] qEnd = new double[dhModels.size()];
        for (int i = 0; i < dhModels.size(); i++) {
            qEnd[i] = dhModels.get(i).getTheta();
            dhModels.get(i).setTheta(qStart[i]);
        }

        if (playbackTimer != null) playbackTimer.stop();

        // 5. Smooth trajectory animation
        final long startTime = System.nanoTime();
        final double durationNs = 2.0 * 1e9; // 2 seconds

        playbackTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                double elapsed = now - startTime;
                double tNorm = elapsed / durationNs;

                if (tNorm >= 1.0) {
                    tNorm = 1.0;
                    stop();
                }

                double[] currentQ = TrajectoryPlanner.interpolateCubic(qStart, qEnd, tNorm);
                for (int i = 0; i < dhModels.size(); i++) {
                    dhModels.get(i).setTheta(currentQ[i]);
                }

                rebuildUIControls();
                updateRobot3D();
            }
        };
        playbackTimer.start();
    }

    private void showWarningDialog(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private VBox createControlPanel(Stage primaryStage) {
        VBox panel = new VBox(10);
        panel.setPrefWidth(340);
        panel.setPadding(new Insets(15));
        panel.setStyle("-fx-background-color: #2b2b36; -fx-text-fill: white;");

        Label header = new Label("Kinematic Chain Setup");
        header.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: white;");

        Button addBtn = new Button("+ Add Joint");
        addBtn.setMaxWidth(Double.MAX_VALUE);
        addBtn.setStyle("-fx-background-color: #98c379; -fx-text-fill: #1e1e24; -fx-font-weight: bold;");
        addBtn.setOnAction(e -> {
            dhModels.add(new DHParameterModel(5.0, 0.0, 0.0, 0.0));
            selectedJointIndex = dhModels.size() - 1;
            rebuildUIControls();
            updateRobot3D();
        });

        // Presets Toolbar
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

        // File I/O Toolbar
        Label fileLabel = new Label("File I/O:");
        fileLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #abb2bf; -fx-font-weight: bold;");

        HBox fileBar = new HBox(8);
        Button exportBtn = new Button("Export JSON");
        exportBtn.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(exportBtn, Priority.ALWAYS);
        exportBtn.setStyle("-fx-background-color: #3b3b4d; -fx-text-fill: #98c379; -fx-border-color: #98c379; -fx-border-radius: 3;");
        exportBtn.setOnAction(e -> exportToJson(primaryStage));

        Button importBtn = new Button("Import JSON");
        importBtn.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(importBtn, Priority.ALWAYS);
        importBtn.setStyle("-fx-background-color: #3b3b4d; -fx-text-fill: #c678dd; -fx-border-color: #c678dd; -fx-border-radius: 3;");
        importBtn.setOnAction(e -> importFromJson(primaryStage));
        fileBar.getChildren().addAll(exportBtn, importBtn);

        // IK Target Controls Panel
        Label ikLabel = new Label("IK Target Workspace:");
        ikLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #abb2bf; -fx-font-weight: bold;");

        VBox ikBox = new VBox(6);
        ikBox.setStyle("-fx-background-color: #21252b; -fx-padding: 10; -fx-background-radius: 5; -fx-border-color: #61afef; -fx-border-radius: 5;");

        HBox targetXRow = createTargetRow("Target X:", targetPos, 0);
        HBox targetYRow = createTargetRow("Target Y:", targetPos, 1);
        HBox targetZRow = createTargetRow("Target Z:", targetPos, 2);

        Button planMotionBtn = new Button("Move to Target (IK + Trajectory)");
        planMotionBtn.setMaxWidth(Double.MAX_VALUE);
        planMotionBtn.setStyle("-fx-background-color: #61afef; -fx-text-fill: #1e1e24; -fx-font-weight: bold;");
        planMotionBtn.setOnAction(e -> runIKAndAnimate());

        ikBox.getChildren().addAll(targetXRow, targetYRow, targetZRow, planMotionBtn);

        controlsContainer = new VBox(12);
        ScrollPane scrollPane = new ScrollPane(controlsContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        panel.getChildren().addAll(
                header, addBtn, presetsLabel, presetBar, fileLabel, fileBar, ikLabel, ikBox,
                new Separator(Orientation.HORIZONTAL), scrollPane
        );

        return panel;
    }

    private HBox createTargetRow(String label, double[] posArray, int index) {
        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_LEFT);

        Label lbl = new Label(label);
        lbl.setPrefWidth(65);
        lbl.setStyle("-fx-text-fill: #abb2bf; -fx-font-size: 11px;");

        Slider slider = new Slider(-25, 25, posArray[index]);
        HBox.setHgrow(slider, Priority.ALWAYS);

        TextField txt = new TextField(String.format("%.2f", posArray[index]));
        txt.setPrefWidth(65);
        txt.setStyle("-fx-background-color: #1e1e24; -fx-text-fill: #e06c75; -fx-font-size: 11px; -fx-border-color: #4b5263; -fx-border-radius: 3;");

        slider.valueProperty().addListener((o, oldV, newV) -> {
            if (!txt.isFocused()) {
                posArray[index] = newV.doubleValue();
                txt.setText(String.format("%.2f", newV.doubleValue()));
                updateTargetSpherePosition();
            }
        });

        Runnable applyTxt = () -> {
            try {
                double parsed = ExpressionParser.parse(txt.getText());
                posArray[index] = parsed;
                slider.setValue(parsed);
                updateTargetSpherePosition();
            } catch (Exception ex) {
                txt.setStyle("-fx-background-color: #1e1e24; -fx-text-fill: #e06c75; -fx-font-size: 11px; -fx-border-color: #e06c75; -fx-border-radius: 3;");
            }
        };

        txt.setOnAction(e -> applyTxt.run());
        txt.focusedProperty().addListener((o, wasF, isF) -> { if (!isF) applyTxt.run(); });

        row.getChildren().addAll(lbl, slider, txt);
        return row;
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

            card.setStyle(bgStyle + " -fx-padding: 10; -fx-background-radius: 5; " + borderStyle);

            card.setOnMouseClicked(e -> {
                selectedJointIndex = index;
                rebuildUIControls();
                updateRobot3D();
            });

            HBox cardHeader = new HBox();
            cardHeader.setAlignment(Pos.CENTER_LEFT);
            Label title = new Label("Joint " + (i + 1) + (isSelected ? " (Selected)" : ""));
            title.setStyle("-fx-font-weight: bold; -fx-text-fill: " + (isSelected ? "#61afef" : "#abb2bf") + ";");

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

            // Inside rebuildUIControls() method of Kinematic3DApp.java

            HBox limitsRow = new HBox(10);
            limitsRow.setAlignment(Pos.CENTER_LEFT);

            Label minLbl = new Label("Min θ:");
            minLbl.setStyle("-fx-text-fill: #abb2bf; -fx-font-size: 10px;");
            TextField minTxt = new TextField(String.format("%.0f", model.getMinTheta()));
            minTxt.setPrefWidth(50);
            minTxt.setStyle("-fx-background-color: #1e1e24; -fx-text-fill: #98c379; -fx-font-size: 10px; -fx-border-color: #4b5263; -fx-border-radius: 3;");

            minTxt.setOnAction(e -> {
                try {
                    double v = ExpressionParser.parse(minTxt.getText());
                    if (minTxt.getText().toLowerCase().contains("pi")) v = Math.toDegrees(v);
                    model.minThetaProperty().set(v);
                } catch (Exception ignored) {}
            });

            Label maxLbl = new Label("Max θ:");
            maxLbl.setStyle("-fx-text-fill: #abb2bf; -fx-font-size: 10px;");
            TextField maxTxt = new TextField(String.format("%.0f", model.getMaxTheta()));
            maxTxt.setPrefWidth(50);
            maxTxt.setStyle("-fx-background-color: #1e1e24; -fx-text-fill: #98c379; -fx-font-size: 10px; -fx-border-color: #4b5263; -fx-border-radius: 3;");

            maxTxt.setOnAction(e -> {
                try {
                    double v = ExpressionParser.parse(maxTxt.getText());
                    if (maxTxt.getText().toLowerCase().contains("pi")) v = Math.toDegrees(v);
                    model.maxThetaProperty().set(v);
                } catch (Exception ignored) {}
            });

            limitsRow.getChildren().addAll(minLbl, minTxt, maxLbl, maxTxt);

            card.getChildren().addAll(
                    cardHeader,
                    createSliderRow("a (Length):", -20, 20, model.aProperty(), false),
                    createSliderRow("α (Twist °):", -180, 180, model.alphaProperty(), true),
                    createSliderRow("d (Offset):", -20, 20, model.dProperty(), false),
                    createSliderRow("θ (Angle °):", model.getMinTheta(), model.getMaxTheta(), model.thetaProperty(), true),
                    limitsRow
            );

            controlsContainer.getChildren().add(card);
        }
    }

    private HBox createSliderRow(String label, double min, double max, DoubleProperty prop, boolean allowPiDegrees) {
        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_LEFT);

        Label lbl = new Label(label);
        lbl.setPrefWidth(75);
        lbl.setStyle("-fx-text-fill: #abb2bf; -fx-font-size: 11px;");

        Slider slider = new Slider(min, max, prop.get());
        HBox.setHgrow(slider, Priority.ALWAYS);

        TextField txtInput = new TextField(String.format("%.2f", prop.get()));
        txtInput.setPrefWidth(65);
        txtInput.setStyle("-fx-background-color: #1e1e24; -fx-text-fill: #e5c07b; -fx-font-size: 11px; -fx-border-color: #4b5263; -fx-border-radius: 3;");

        slider.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (!txtInput.isFocused()) {
                prop.set(newVal.doubleValue());
                txtInput.setText(String.format("%.2f", newVal.doubleValue()));
                updateRobot3D();
            }
        });

        Runnable applyTextVal = () -> {
            try {
                String rawText = txtInput.getText();
                double parsedVal = ExpressionParser.parse(rawText);

                if (allowPiDegrees && rawText.toLowerCase().contains("pi")) {
                    parsedVal = Math.toDegrees(parsedVal);
                }

                prop.set(parsedVal);
                slider.setValue(parsedVal);
                txtInput.setStyle("-fx-background-color: #1e1e24; -fx-text-fill: #e5c07b; -fx-font-size: 11px; -fx-border-color: #4b5263; -fx-border-radius: 3;");
                updateRobot3D();
            } catch (Exception ex) {
                txtInput.setStyle("-fx-background-color: #1e1e24; -fx-text-fill: #e06c75; -fx-font-size: 11px; -fx-border-color: #e06c75; -fx-border-radius: 3;");
            }
        };

        txtInput.setOnAction(e -> applyTextVal.run());
        txtInput.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if (!isFocused) applyTextVal.run();
        });

        row.getChildren().addAll(lbl, slider, txtInput);
        return row;
    }

    private void updateRobot3D() {
        robotGroup.getChildren().clear();
        robotGroup.getChildren().add(targetSphere);

        List<DHParameter> dhParams = new ArrayList<>();
        for (DHParameterModel model : dhModels) {
            dhParams.add(model.toDHParameter());
        }

        List<Matrix4x4> transforms = fkEngine.computeCumulativeTransforms(dhParams);

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

    private void loadScaraPreset() {
        dhModels.clear();
        dhModels.add(new DHParameterModel(10.0, 0.0, 0.0, 0.0));
        dhModels.add(new DHParameterModel(8.0, 180.0, 0.0, 0.0));
        dhModels.add(new DHParameterModel(0.0, 0.0, 5.0, 0.0));
        selectedJointIndex = 0;
        rebuildUIControls();
        updateRobot3D();
    }

    private void loadPuma560Preset() {
        dhModels.clear();
        dhModels.add(new DHParameterModel(0.0, -90.0, 0.0, 0.0));
        dhModels.add(new DHParameterModel(8.0, 0.0, 0.0, -30.0));
        dhModels.add(new DHParameterModel(2.0, 90.0, 0.0, 45.0));
        dhModels.add(new DHParameterModel(0.0, -90.0, 8.0, 0.0));
        dhModels.add(new DHParameterModel(0.0, 90.0, 0.0, 30.0));
        dhModels.add(new DHParameterModel(0.0, 0.0, 2.0, 0.0));
        selectedJointIndex = 0;
        rebuildUIControls();
        updateRobot3D();
    }

    private void exportToJson(Stage stage) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export DH Table to JSON");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON Files (*.json)", "*.json"));
        fileChooser.setInitialFileName("robot_dh_config.json");

        File file = fileChooser.showSaveDialog(stage);
        if (file == null) return;

        StringBuilder json = new StringBuilder("[\n");
        for (int i = 0; i < dhModels.size(); i++) {
            DHParameterModel model = dhModels.get(i);
            json.append(String.format("  { \"a\": %.4f, \"alpha\": %.4f, \"d\": %.4f, \"theta\": %.4f }",
                    model.getA(), model.getAlpha(), model.getD(), model.getTheta()));
            if (i < dhModels.size() - 1) json.append(",");
            json.append("\n");
        }
        json.append("]");

        try (PrintWriter writer = new PrintWriter(file)) {
            writer.write(json.toString());
        } catch (Exception e) {
            showErrorDialog("Export Error", "Failed to save file: " + e.getMessage());
        }
    }

    private void importFromJson(Stage stage) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Import DH Table from JSON");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON Files (*.json)", "*.json"));

        File file = fileChooser.showOpenDialog(stage);
        if (file == null) return;

        try {
            String content = Files.readString(file.toPath()).trim();
            if (!content.startsWith("[") || !content.endsWith("]")) {
                throw new IllegalArgumentException("Invalid JSON format.");
            }

            List<DHParameterModel> newModels = new ArrayList<>();
            String inner = content.substring(1, content.length() - 1).trim();
            String[] objects = inner.split("(?<=\\}),\\s*(?=\\{)");

            for (String objStr : objects) {
                double a = extractJsonDouble(objStr, "a");
                double alpha = extractJsonDouble(objStr, "alpha");
                double d = extractJsonDouble(objStr, "d");
                double theta = extractJsonDouble(objStr, "theta");
                newModels.add(new DHParameterModel(a, alpha, d, theta));
            }

            if (!newModels.isEmpty()) {
                dhModels.clear();
                dhModels.addAll(newModels);
                selectedJointIndex = 0;
                rebuildUIControls();
                updateRobot3D();
            }
        } catch (Exception e) {
            showErrorDialog("Import Error", "Failed to load DH table: " + e.getMessage());
        }
    }

    private double extractJsonDouble(String jsonObj, String key) {
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\"" + key + "\"\\s*:\\s*([-+]?[0-9]*\\.?[0-9]+)");
        java.util.regex.Matcher matcher = pattern.matcher(jsonObj);
        return matcher.find() ? Double.parseDouble(matcher.group(1)) : 0.0;
    }

    private void showErrorDialog(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}