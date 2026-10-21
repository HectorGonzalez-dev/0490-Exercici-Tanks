package com.client;

import java.net.URL;
import java.util.ResourceBundle;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;

import com.shared.GameSnapshot;

public class CtrlPlay implements Initializable {

    @FXML private Canvas canvas;
    @FXML private Label lblPlayer1;
    @FXML private Label lblPlayer2;

    private GraphicsContext gc;
    private GameSnapshot currentState;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        gc = canvas.getGraphicsContext2D();
        // TODO: configurar events de teclat, ratoli, timer de dibuix
    }

    public void updateState(GameSnapshot state) {
        this.currentState = state;
        // TODO: redibuixar
    }

    private void draw() {
        // TODO: dibuixar estat del joc (parets, tancs, bales, explosions, HUD)
    }
}
