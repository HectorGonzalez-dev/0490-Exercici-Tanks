package com.client;

import java.net.URL;
import java.util.ResourceBundle;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;

public class CtrlResult implements Initializable {

    @FXML public Label txtWinner;
    @FXML public Label txtLoser;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
    }

    @FXML
    private void playAgain() {
        // TODO: enviar playAgain al servidor, tornar a vista espera
    }
}
