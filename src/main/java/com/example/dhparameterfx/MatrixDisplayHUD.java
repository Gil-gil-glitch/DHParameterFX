package com.example.dhparameterfx;

import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

public class MatrixDisplayHUD extends VBox {

    private final Label posLabel = new Label("End-Effector Pos: (0.00, 0.00, 0.00)");
    private final Label[][] matrixCells = new Label[4][4];

    public MatrixDisplayHUD() {

        setSpacing(8);
        setPadding(new Insets(12));
        setStyle("-fx-background-color: rgba(30, 30, 36, 0.85); " +
                "-fx-border-color: #61afef; " +
                "-fx-border-width: 1px; " +
                "-fx-border-radius: 6; " +
                "-fx-background-radius: 6;");
        setMaxSize(USE_PREF_SIZE, USE_PREF_SIZE);

        Label header = new Label("END-EFFECTOR STATE (T₀ⁿ)");
        header.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #61afef;");

        posLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #e5c07b;");

        GridPane matrixGrid = new GridPane();
        matrixGrid.setHgap(8);
        matrixGrid.setVgap(4);

        for (int r = 0; r < 4; r++) {

            for (int c = 0; c < 4; c++) {

                Label cell = new Label("0.00");
                cell.setPrefWidth(52);
                cell.setStyle("-fx-font-family: monospace; -fx-font-size: 10px; -fx-text-fill: #abb2bf; -fx-alignment: center-right;");
                matrixCells[r][c] = cell;
                matrixGrid.add(cell, c, r);

            }

        }

        getChildren().addAll(header, posLabel, matrixGrid);
    }

    public void update(Matrix4x4 finalTransform) {

        double[] pos = finalTransform.getPosition();
        posLabel.setText(String.format("End-Effector Pos: (%.2f, %.2f, %.2f)", pos[0], pos[1], pos[2]));

        for (int r = 0; r < 4; r++) {

            for (int c = 0; c < 4; c++) {
                matrixCells[r][c].setText(String.format("%6.2f", finalTransform.get(r, c)));

            }

        }
    }
}