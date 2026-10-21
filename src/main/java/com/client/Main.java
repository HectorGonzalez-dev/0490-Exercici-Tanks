package com.client;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.animation.PauseTransition;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.util.Duration;

import org.json.JSONObject;

public class Main extends Application {

    public static UtilsWS wsClient;
    public static String clientName = "";

    public static CtrlConfig ctrlConfig;
    public static CtrlWait ctrlWait;
    public static CtrlPlay ctrlPlay;
    public static CtrlResult ctrlResult;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) throws Exception {
        // TODO: registrar las 4 vistas, obtener controllers, mostrar escena
    }

    @Override
    public void stop() {
        if (wsClient != null) {
            wsClient.forceExit();
        }
        System.exit(1);
    }

    public static void connectToServer() {
        // TODO: crear UtilsWS, connectar, registrar callbacks
    }

    private static void wsMessage(String response) {
        // TODO: parsear JSON, redirigir a la vista correspondiente
    }

    private static void wsError(String response) {
        // TODO: mostrar error de conexion
    }

    public static void pauseDuring(long milliseconds, Runnable action) {
        PauseTransition pause = new PauseTransition(Duration.millis(milliseconds));
        pause.setOnFinished(event -> Platform.runLater(action));
        pause.play();
    }
}
