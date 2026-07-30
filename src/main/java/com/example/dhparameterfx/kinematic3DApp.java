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

import javafx.scene.shape.Box;
import javafx.scene.shape.DrawMode;
import javafx.scene.shape.CullFace;


public class kinematic3DApp extends Application {

    private double dragMouseX;
    private double dragMouseY;
    private GizmoData activeGizmo = null;

    private final Group world = new Group();
    private final Group robotGroup = new Group(); // Container specifically for robot geometry
    private final ForwardKinematicsEngine fkEngine = new ForwardKinematicsEngine();

    // Active model parameters
    private final List<DHParameterModel> dhModels = new ArrayList<>();
    private VBox controlsContainer;

    private int selectedJointIndex = 0;

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

        // Set up Default 3 Joint Robot
        dhModels.add(new DHParameterModel(0.0, 90.0, 5.0, 30.0));   // Joint 1
        dhModels.add(new DHParameterModel(10.0, 0.0, 0.0, 40.0));  // Joint 2
        dhModels.add(new DHParameterModel(8.0, 0.0, 0.0, -25.0));   // Joint 3

        BorderPane root = new BorderPane();
        root.setCenter(subScene);

        subScene.widthProperty().bind(root.widthProperty().subtract(320));
        subScene.heightProperty().bind(root.heightProperty());

        // Side Control Panel
        VBox sidePanel = createControlPanel();
        root.setRight(sidePanel);

        Scene scene = new Scene(root, 1120, 700);

        subScene.setOnMouseClicked(event -> {
            Node picked = event.getPickResult().getIntersectedNode();

            // Walk up the node hierarchy to find if we clicked inside an AxisGroup
            Node current = picked;
            while (current != null && !(current instanceof AxisGroup) && current != robotGroup) {
                current = current.getParent();
            }

            if (current instanceof AxisGroup axis) {

                // Tag stored on the AxisGroup (or get index from loop)
                Object tag = axis.getUserData();

                if (tag instanceof Integer jointIdx) {

                    selectedJointIndex = jointIdx;
                    rebuildUIControls();
                    updateRobot3D();

                }

            }

        });

        scene.setOnMouseDragged(event -> {

            if (activeGizmo != null) {

                double deltaX = event.getSceneX() - dragMouseX;
                double deltaY = event.getSceneY() - dragMouseY;

                double dragAmount = (Math.abs(deltaX) > Math.abs(deltaY)) ? deltaX : -deltaY;

                DHParameterModel model = dhModels.get(activeGizmo.jointIndex());
                double sensitivity = 0.1;

                switch (activeGizmo.type()) {
                    case DRAG_A -> model.setA(model.getA() + dragAmount * sensitivity);
                    case DRAG_D -> model.setD(model.getD() + dragAmount * sensitivity);
                    case ROTATE_THETA -> model.setTheta(model.getTheta() + dragAmount * sensitivity * 2);
                    case ROTATE_ALPHA -> model.setAlpha(model.getAlpha() + dragAmount * sensitivity * 2);
                }

                dragMouseX = event.getSceneX();
                dragMouseY = event.getSceneY();

                // Force UI rebuild + 3D update on drag
                rebuildUIControls();
                updateRobot3D();

                event.consume(); // Blocks the orbit camera while dragging a gizmo

            }
        });

        // Register Camera AFTER gizmo listeners
        cameraRig.registerMouseEvents(scene);

        rebuildUIControls();
        updateRobot3D();

        primaryStage.setTitle("JavaFX DH Parameter Calculator");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    /**
     * Creates a blue wireframe bounding box to visually highlight the selected joint.
     */
    private Node createSelectionHighlightBox(double size) {
        Box box = new Box(size, size, size);
        box.setDrawMode(DrawMode.LINE); // Wireframe outline
        box.setCullFace(CullFace.NONE);

        PhongMaterial blueWireframe = new PhongMaterial(Color.web("#61afef"));
        box.setMaterial(blueWireframe);

        return box;
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

            // Highlight active card with a blue border and darker background
            boolean isSelected = (i == selectedJointIndex);
            String borderStyle = isSelected ? "-fx-border-color: #61afef; -fx-border-width: 2px; -fx-border-radius: 5;" : "";
            String bgStyle = isSelected ? "-fx-background-color: #2c313a;" : "-fx-background-color: #3b3b4d;";

            card.setStyle(bgStyle + " -fx-padding: 10; -fx-background-radius: 5; " + borderStyle);

            // Clicking card also selects joint in 3D
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

        List<DHParameter> dhParams = new ArrayList<>();
        for (DHParameterModel model : dhModels) {
            dhParams.add(model.toDHParameter());
        }

        List<Matrix4x4> transforms = fkEngine.computeCumulativeTransforms(dhParams);

        for (int i = 0; i < transforms.size(); i++) {
            Matrix4x4 mat = transforms.get(i);

            // Render RGB Axis Frame
            AxisGroup axis = new AxisGroup(3.0, 0.2, i);
            axis.setUserData(i); // Tag the entire group with joint index
            applyMatrixToNode(axis, mat);
            robotGroup.getChildren().add(axis);

            // Render Selection Box on the currently selected joint
            if (i == selectedJointIndex) {
                Node highlightBox = createSelectionHighlightBox(4.5);
                applyMatrixToNode(highlightBox, mat);
                robotGroup.getChildren().add(highlightBox);
            }

            // Render Link Cylinder connecting to previous joint
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