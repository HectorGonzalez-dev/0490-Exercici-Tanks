package com.client;

import java.net.URL;
import java.util.ResourceBundle;

import org.json.JSONObject;

import com.shared.Protocol;

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
        JSONObject msg = new JSONObject();
        msg.put("type", Protocol.PLAY_AGAIN);
        if (Main.wsClient != null) Main.wsClient.safeSend(msg.toString());

        // Al volver a la espera, el título debe volver a decir "Esperant jugadors ..."
        // (puede haber quedado el "GO!" de la cuenta atrás anterior)
        Main.ctrlWait.txtTitle.setText("Esperant jugadors ...");

        UtilsViews.setViewAnimating("ViewWait");
    }
}
