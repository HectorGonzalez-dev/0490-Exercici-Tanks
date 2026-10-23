package com.server;

import com.shared.GameConfig;

public class ServerBullet {

    public static final double RADIUS = GameConfig.BULLET_RADIUS;

    public final String owner;      // Nombre del jugador que disparó
    public double x;
    public double y;
    public double angle;            // Grados
    public boolean bounced;         // Si ya rebotó una vez (máximo 1)

    public ServerBullet(String owner, double x, double y, double angle) {
        this.owner = owner;
        this.x = x;
        this.y = y;
        this.angle = angle;
        this.bounced = false;
    }

    /** Devuelve la posición después de dt segundos, sin mover el objeto aún. */
    public Point2DServer nextPosition(double dt) {
        return ServerMath.positionAtDistance(
            new Point2DServer(x, y), angle, GameConfig.BULLET_SPEED * dt);
    }

    /** Rebotar contra una pared. */
    public void bounce(boolean invertX, boolean invertY) {
        Point2DServer dir = ServerMath.directionForAngle(angle);
        double dx = invertX ? -dir.x : dir.x;
        double dy = invertY ? -dir.y : dir.y;
        angle = ServerMath.angleForMovement(dx, dy);
        bounced = true;
    }
}