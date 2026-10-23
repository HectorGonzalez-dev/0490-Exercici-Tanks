package com.server;

public final class ServerMath {

    private ServerMath() { }

    // Ángulo según la dirección (dx, dy). 0° = derecha, 90° = abajo.
    public static double angleForMovement(double dx, double dy) {
        return Math.toDegrees(Math.atan2(dy, dx));
    }

    // Desplazamiento unitario según un ángulo en grados.
    public static Point2DServer directionForAngle(double angle) {
        double radians = Math.toRadians(angle);
        return new Point2DServer(Math.cos(radians), Math.sin(radians));
    }

    // Punto a una distancia dada, siguiendo un ángulo desde una posición.
    public static Point2DServer positionAtDistance(Point2DServer position, double angle, double distance) {
        Point2DServer dir = directionForAngle(angle);
        return new Point2DServer(position.x + dir.x * distance, position.y + dir.y * distance);
    }
}