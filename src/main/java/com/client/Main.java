package com.client;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.animation.PauseTransition;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.util.Duration;

import org.json.JSONObject;

import com.shared.GameSnapshot;
import com.shared.Protocol;

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

        final int windowWidth = 800;
        final int windowHeight = 560;

        // 1. Registrar les 4 vistes (la primera és la que es mostra al iniciar)
        UtilsViews.parentContainer.setStyle("-fx-font: 14 arial;");
        UtilsViews.addView(getClass(), "ViewConfig", "/assets/viewConfig.fxml");
        UtilsViews.addView(getClass(), "ViewWait", "/assets/viewWait.fxml");
        UtilsViews.addView(getClass(), "ViewPlay", "/assets/viewPlay.fxml");
        UtilsViews.addView(getClass(), "ViewResult", "/assets/viewResult.fxml");

        // 2. Guardar referència als controladors de cada vista
        ctrlConfig = (CtrlConfig) UtilsViews.getController("ViewConfig");
        ctrlWait = (CtrlWait) UtilsViews.getController("ViewWait");
        ctrlPlay = (CtrlPlay) UtilsViews.getController("ViewPlay");
        ctrlResult = (CtrlResult) UtilsViews.getController("ViewResult");

        // 3. Crear l'escena amb el contenidor de vistes i mostrar la finestra
        Scene scene = new Scene(UtilsViews.parentContainer);

        stage.setScene(scene);
        stage.setTitle("Tanks");
        stage.setMinWidth(windowWidth);
        stage.setMinHeight(windowHeight);
        stage.setResizable(false);
        stage.show();

        // 4. Icona de finestra (no funciona a Mac)
        if (!System.getProperty("os.name").contains("Mac")) {
            Image icon = new Image(getClass().getResourceAsStream("/icons/icon.png"));
            stage.getIcons().add(icon);
        }
    }

    @Override
    public void stop() {
        if (wsClient != null) {
            wsClient.forceExit();
        }
        System.exit(1);
    }

    public static void connectToServer() {

        String name = ctrlConfig.txtName.getText().trim();
        if (name.isEmpty()) name = "Jugador";
        final String playerName = name;

        ctrlConfig.txtMessage.setStyle("-fx-text-fill: black;");
        ctrlConfig.txtMessage.setText("Connectant ...");

        pauseDuring(1500, () -> {
            String protocol = ctrlConfig.txtProtocol.getText();
            String host = ctrlConfig.txtHost.getText();
            String port = ctrlConfig.txtPort.getText();

            wsClient = UtilsWS.getSharedInstance(protocol + "://" + host + ":" + port);

            wsClient.onMessage((response) -> { Platform.runLater(() -> { wsMessage(response); }); });
            wsClient.onError((response) -> { Platform.runLater(() -> { wsError(response); }); });

            pauseDuring(500, () -> {
                JSONObject msg = new JSONObject();
                msg.put("type", Protocol.SET_NAME);
                msg.put("name", playerName);
                if (wsClient != null) wsClient.safeSend(msg.toString());
            });
        });
    }

    private static void wsMessage(String response) {

        // 1. Convertir el String a JSONObject
        JSONObject msgObj = new JSONObject(response);

        // 2. Llegir el camp "type" per saber quin tipus de missatge és
        String type = msgObj.optString("type", "");

        switch (type) {

            // El servidor ens diu que estem esperant un contrincant
            case Protocol.WAITING: {
                // Guardar el nostre nom assignat pel servidor
                clientName = msgObj.optString("clientName", "");

                // Mostrar els noms dels jugadors a la vista d'espera
                String player0 = msgObj.optString("player0", "?");
                String player1 = msgObj.optString("player1", "?");
                ctrlWait.txtPlayer0.setText(player0);
                ctrlWait.txtPlayer1.setText(player1);

                // Si estem a la vista Config, canviar a la vista Wait
                if (UtilsViews.getActiveView().equals("ViewConfig")) {
                    UtilsViews.setViewAnimating("ViewWait");
                }
                break;
            }

            // El servidor envia el compte enrere (5, 4, 3, 2, 1, 0)
            case Protocol.COUNTDOWN: {
                int value = msgObj.getInt("value");

                if (value == 0) {
                    // Quan arriba a 0  mostrar go  i canviar a la vista de joc
                    ctrlWait.txtTitle.setText("GO!");
                    pauseDuring(500, () -> {
                        UtilsViews.setViewAnimating("ViewPlay");
                    });
                } else {
                    // Mostrar el numero del compte enrere
                    ctrlWait.txtTitle.setText(String.valueOf(value));
                }
                break;
            }

            //servidor envia lestat del joc 60 vegades per segon
            case Protocol.GAME_STATE: {
                // Convertir el JSON a GameSnapshot i passar a la vista de joc
                GameSnapshot snapshot = GameSnapshot.fromJSON(msgObj);
                ctrlPlay.updateState(snapshot);
                break;
            }

            // La partida ha acabat
            case Protocol.GAME_OVER: {
                // Mostrar guanyador i perdedor a la vista de resultat
                String winner = msgObj.optString("winner", "?");
                String loser = msgObj.optString("loser", "?");
                ctrlResult.txtWinner.setText("Guanyador: " + winner);
                ctrlResult.txtLoser.setText("Perdedor: " + loser);

                // Canviar a la vista de resultat
                UtilsViews.setViewAnimating("ViewResult");
                break;
            }
        }
    }

    private static void wsError(String response) {

        // Si el servidor no està engegat, mostrar l'error en vermell
        if (response.contains("Connection refused")) {
            ctrlConfig.txtMessage.setStyle("-fx-text-fill: red;");
            ctrlConfig.txtMessage.setText("Connection refused");

            // Esborrar el missatge d'error al cap d'1.5 segons
            pauseDuring(1500, () -> {
                ctrlConfig.txtMessage.setText("");
            });
        }
    }

    public static void pauseDuring(long milliseconds, Runnable action) {
        PauseTransition pause = new PauseTransition(Duration.millis(milliseconds));
        pause.setOnFinished(event -> Platform.runLater(action));
        pause.play();
    }
}
