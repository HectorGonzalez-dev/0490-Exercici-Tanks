package com.server;

import com.shared.GameConfig;

public class ServerTank {

    public static final double RADIUS = GameConfig.TANK_RADIUS;

    public final String id;      // "0" o "1" — identidad estable del jugador
    public String name;          // nombre del jugador (editable: llega por setName)
    public final String color;

    public double x;
    public double y;
    public double angle;        // Orientación del cuerpo (grados)
    public double turretAngle;  // Orientación de la torreta (grados)
    public int lives;
    public boolean alive;

    public double speed;        // px/s; 0 = parado

    public ServerTank(String id, String name, String color,
                      double x, double y, double angle, double turretAngle,
                      int lives, boolean alive) {
        this.id = id;
        this.name = name;
        this.color = color;
        this.x = x;
        this.y = y;
        this.angle = angle;
        this.turretAngle = turretAngle;
        this.lives = lives;
        this.alive = alive;
        this.speed = 0;
    }

    /**
     * El tanque recibe un impacto: pierde una vida y deja de estar vivo.
     *
     * @return true si se quedó sin vidas (el rival gana la partida),
     *         false si aún le quedan vidas y reaparecerá en la próxima ronda.
     */
    public boolean loseLife() {
        lives--;
        if (lives <= 0) {
            lives = 0;
            alive = false;
            speed = 0;
            return true; // sin vidas: fin de partida para este jugador
        }
        alive = false;
        speed = 0;
        return false; // perderá una vida y reaparecerá al iniciar la siguiente ronda
    }

    /** Actualiza ángulo y velocidad a partir de una dirección de movimiento (dx, dy). */
    public void updateMovement(double dx, double dy, double speed) {
        if (dx == 0 && dy == 0) {
            this.speed = 0;
            return;
        }
        this.angle = ServerMath.angleForMovement(dx, dy);
        this.speed = speed;
    }

    /** Apunta la torreta hacia una posición objetivo. */
    public void pointAt(double targetX, double targetY) {
        this.turretAngle = ServerMath.angleForMovement(targetX - x, targetY - y);
    }

    /** Devuelve la posición después de dt segundos, sin mover el objeto aún. */
    public Point2DServer nextPosition(double dt) {
        return ServerMath.positionAtDistance(
            new Point2DServer(x, y), angle, speed * dt);
    }
}