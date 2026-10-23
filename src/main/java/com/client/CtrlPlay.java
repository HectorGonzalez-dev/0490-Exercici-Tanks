package com.client;

import java.net.URL;
import java.util.EnumSet;
import java.util.ResourceBundle;
import java.util.Set;

import org.json.JSONObject;

import com.shared.GameSnapshot;
import com.shared.Protocol;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;

public class CtrlPlay implements Initializable {

    @FXML private Canvas canvas;
    @FXML private Label lblPlayer1;
    @FXML private Label lblPlayer2;

    private GraphicsContext gc;
    private GameSnapshot currentState;

    private double mouseX, mouseY;
    private final Set<KeyCode> pressedKeys = EnumSet.noneOf(KeyCode.class);

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // guardamos el contexto grafico del canvas para poder dibujar
        gc = canvas.getGraphicsContext2D();

        // runLater porque la escena y la ventana aun no existen cuando se ejecuta initialize
        Platform.runLater(() -> {

            // cuando el jugador pulsa una tecla, la añadimos al set de teclas pulsadas
            // .add() devuelve true solo si la tecla no estaba ya, asi evitamos enviar mensajes repetidos
            canvas.getScene().setOnKeyPressed(event -> {
                if (pressedKeys.add(event.getCode())) {
                    sendMovement();
                }
            });

            // cuando suelta una tecla, la quitamos del set
            // .remove() devuelve true solo si estaba, asi evitamos envios innecesarios
            canvas.getScene().setOnKeyReleased(event -> {
                if (pressedKeys.remove(event.getCode())) {
                    sendMovement();
                }
            });

            // si el jugador cambia de ventana (alt+tab), limpiamos todas las teclas
            // y enviamos STOP para que el tanque no se quede moviendose solo
            canvas.getScene().getWindow().focusedProperty().addListener((obs, old, focused) -> {
                if (!focused) {
                    pressedKeys.clear();
                    sendMove("STOP");
                }
            });
        });

        // cuando el raton se mueve por el canvas, guardamos la posicion y la enviamos al servidor
        canvas.setOnMouseMoved(this::onMouseMoved);
        // lo mismo si se mueve mientras se mantiene un boton pulsado (arrastrando)
        canvas.setOnMouseDragged(this::onMouseMoved);
        // cuando se hace clic, apuntamos y disparamos
        canvas.setOnMousePressed(this::onMousePressed);
    }

    // se ejecuta cada vez que el raton se mueve sobre el canvas
    private void onMouseMoved(MouseEvent event) {
        // guardamos la posicion del raton
        mouseX = event.getX();
        mouseY = event.getY();
        // enviamos al servidor hacia donde apunta la torreta
        sendAim(mouseX, mouseY);
    }

    // se ejecuta cuando el jugador hace clic sobre el canvas
    private void onMousePressed(MouseEvent event) {
        mouseX = event.getX();
        mouseY = event.getY();
        // primero actualizamos la direccion de apuntado
        sendAim(mouseX, mouseY);
        // si es clic izquierdo, disparamos
        if (event.getButton() == MouseButton.PRIMARY) {
            sendFire();
        }
    }

    // convierte las teclas pulsadas en direcciones y las envia al servidor
    private void sendMovement() {
        double dx = 0, dy = 0;

        // flechas o wasd, ambos funcionan
        if (pressedKeys.contains(KeyCode.LEFT) || pressedKeys.contains(KeyCode.A)) dx -= 1;
        if (pressedKeys.contains(KeyCode.RIGHT) || pressedKeys.contains(KeyCode.D)) dx += 1;
        if (pressedKeys.contains(KeyCode.UP) || pressedKeys.contains(KeyCode.W)) dy -= 1;
        if (pressedKeys.contains(KeyCode.DOWN) || pressedKeys.contains(KeyCode.S)) dy += 1;

        // si no hay teclas pulsadas, paramos el tanque
        if (dx == 0 && dy == 0) {
            sendMove("STOP");
        } else {
            // enviamos cada direccion activa por separado
            if (dy < 0) sendMove("FORWARD");
            if (dy > 0) sendMove("BACKWARD");
            if (dx < 0) sendMove("LEFT");
            if (dx > 0) sendMove("RIGHT");
        }
    }

    // envia un mensaje json al servidor con la direccion de movimiento
    private void sendMove(String direction) {
        JSONObject msg = new JSONObject();
        msg.put("type", Protocol.MOVE);
        msg.put("direction", direction);
        if (Main.wsClient != null) Main.wsClient.safeSend(msg.toString());
    }

    // envia al servidor la posicion donde apunta el raton (para la torreta)
    private void sendAim(double x, double y) {
        JSONObject msg = new JSONObject();
        msg.put("type", Protocol.AIM);
        msg.put("x", x);
        msg.put("y", y);
        if (Main.wsClient != null) Main.wsClient.safeSend(msg.toString());
    }

    // envia al servidor la orden de disparar
    private void sendFire() {
        JSONObject msg = new JSONObject();
        msg.put("type", Protocol.FIRE);
        if (Main.wsClient != null) Main.wsClient.safeSend(msg.toString());
    }

    public void updateState(GameSnapshot state) {
        this.currentState = state;
        // TODO: redibuixar
    }

    private void draw() {
        // TODO: dibuixar estat del joc (parets, tancs, bales, explosions, HUD)
    }
}
