package com.client;

import java.net.URL;
import java.util.EnumSet;
import java.util.ResourceBundle;
import java.util.Set;

import org.json.JSONObject;

import com.shared.BulletState;
import com.shared.ExplosionState;
import com.shared.GameConfig;
import com.shared.GameSnapshot;
import com.shared.Protocol;
import com.shared.TankState;
import com.shared.WallState;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;

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

    // cada vez que el servidor envia un nuevo estado, lo guardamos y redibujamos
    public void updateState(GameSnapshot state) {
        this.currentState = state;
        draw();
    }

    private void draw() {
        if (currentState == null) return;

        double w = canvas.getWidth();
        double h = canvas.getHeight();

        // 1. borrar todo el canvas pintando el fondo
        gc.setFill(Color.WHITESMOKE);
        gc.fillRect(0, 0, w, h);

        // 2. dibujar las paredes
        for (WallState wall : currentState.walls) {
            gc.setFill(Color.web(wall.color));
            gc.fillRect(wall.x, wall.y, wall.width, wall.height);
        }

        // 3. dibujar los tanques
        for (TankState tank : currentState.tanks) {
            if (!tank.alive) continue;
            drawTank(gc, tank);
        }

        // 4. dibujar las balas
        for (BulletState bullet : currentState.bullets) {
            drawBullet(gc, bullet);
        }

        // 5. dibujar las explosiones
        for (ExplosionState explosion : currentState.explosions) {
            drawExplosion(gc, explosion);
        }

        // 6. dibujar la guia de puntos y la mira del raton
        TankState myTank = currentState.getMyTank();
        if (myTank != null && myTank.alive) {
            drawGuide(gc, myTank);
            drawCrosshair(gc, myTank);
        }

        // 7. actualizar el hud (nombres y vidas)
        updateHUD();
    }

    // dibuja el cuerpo del tanque y la torreta
    private void drawTank(GraphicsContext gc, TankState tank) {
        // cuerpo del tanque: dos cadenas de oruga + rectangulo central
        gc.save();
        gc.translate(tank.x, tank.y);
        gc.rotate(tank.angle);
        // cadenas de oruga (gris oscuro)
        gc.setFill(Color.DARKSLATEGRAY);
        gc.fillRect(-18, -17, 36, 6);
        gc.fillRect(-18, 11, 36, 6);
        // cuerpo central (color del jugador)
        gc.setFill(Color.web(tank.color));
        gc.fillRect(-16, -11, 32, 22);
        gc.restore();

        // torreta: canon + circulo central
        gc.save();
        gc.translate(tank.x, tank.y);
        gc.rotate(tank.turretAngle);
        // canon (color mas oscuro)
        gc.setFill(Color.web(tank.color).darker());
        gc.fillRect(0, -3, 32, 6);
        // base de la torreta
        gc.fillOval(-9, -9, 18, 18);
        gc.restore();
    }

    // dibuja una bala como rectangulo + circulo rojo, rotada segun su angulo
    private void drawBullet(GraphicsContext gc, BulletState bullet) {
        gc.save();
        gc.translate(bullet.x, bullet.y);
        gc.rotate(bullet.angle);
        gc.setFill(Color.RED);
        gc.fillRect(-4, -2, 7, 4);
        gc.fillOval(1, -2, 4, 4);
        gc.restore();
    }

    // dibuja una explosion como un circulo naranja que crece y se desvanece
    private void drawExplosion(GraphicsContext gc, ExplosionState explosion) {
        // el servidor nos envia el progreso (0.0 a 1.0)
        double progress = explosion.progress;
        // el radio crece del 30% al 100% del radio maximo
        double currentRadius = explosion.radius * (0.3 + 0.7 * progress);
        double x = explosion.x - currentRadius;
        double y = explosion.y - currentRadius;

        gc.save();
        // la opacidad baja a medida que avanza la explosion
        gc.setGlobalAlpha(1 - progress);
        gc.setFill(Color.ORANGE);
        gc.fillOval(x, y, currentRadius * 2, currentRadius * 2);
        gc.setStroke(Color.ORANGERED);
        gc.strokeOval(x, y, currentRadius * 2, currentRadius * 2);
        gc.restore();
    }

    // dibuja la linea de puntos desde el tanque hasta el raton
    // se para si un punto toca una pared
    private void drawGuide(GraphicsContext gc, TankState myTank) {
        double startX = myTank.x;
        double startY = myTank.y;

        // calcular el angulo desde el tanque hasta el raton
        double dx = mouseX - startX;
        double dy = mouseY - startY;
        double angle = Math.toDegrees(Math.atan2(dy, dx));
        double distance = Math.sqrt(dx * dx + dy * dy);

        // convertir el angulo a radianes para calcular las posiciones de los puntos
        double rad = Math.toRadians(angle);
        double dotRadius = 3;

        gc.save();
        gc.setFill(Color.web(myTank.color));

        // dibujar un punto cada 18 pixeles, empezando a 44px del centro del tanque
        for (double d = 44; d < distance; d += 18) {
            double dotX = startX + Math.cos(rad) * d;
            double dotY = startY + Math.sin(rad) * d;

            // si el punto toca una pared, dejamos de dibujar
            boolean blocked = false;
            for (WallState wall : currentState.walls) {
                if (dotX + dotRadius > wall.x && dotX - dotRadius < wall.x + wall.width &&
                    dotY + dotRadius > wall.y && dotY - dotRadius < wall.y + wall.height) {
                    blocked = true;
                    break;
                }
            }
            if (blocked) break;

            gc.fillOval(dotX - dotRadius, dotY - dotRadius, dotRadius * 2, dotRadius * 2);
        }

        gc.restore();
    }

    // dibuja la mira (crosshair) en la posicion del raton
    private void drawCrosshair(GraphicsContext gc, TankState myTank) {
        double x = mouseX;
        double y = mouseY;

        gc.save();
        gc.setStroke(Color.web(myTank.color));
        gc.setLineWidth(2);
        // cuatro lineas formando una cruz con un hueco en el centro
        gc.strokeLine(x - 12, y, x - 4, y);
        gc.strokeLine(x + 4, y, x + 12, y);
        gc.strokeLine(x, y - 12, x, y - 4);
        gc.strokeLine(x, y + 4, x, y + 12);
        gc.restore();
    }

    // actualiza los labels de la parte superior con nombres y vidas
    private void updateHUD() {
        TankState tank0 = currentState.getTank(0);
        TankState tank1 = currentState.getTank(1);

        if (tank0 != null) {
            int bullets0 = currentState.getActiveBullets(0);
            lblPlayer1.setText(tank0.name + "  " + "❤".repeat(tank0.lives) + "  💣:" + bullets0 + "/" + currentState.maxBullets);
        }
        if (tank1 != null) {
            int bullets1 = currentState.getActiveBullets(1);
            lblPlayer2.setText(tank1.name + "  " + "❤".repeat(tank1.lives) + "  💣:" + bullets1 + "/" + currentState.maxBullets);
        }
    }
}
