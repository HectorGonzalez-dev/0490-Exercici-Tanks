package com.server;

public final class ServerMath {

    private ServerMath() { }

    /** Punto 2D puro (sin JavaFX) para cálculos del servidor. */
    public static final class Point2DServer {
        public final double x;
        public final double y;

        public Point2DServer(double x, double y) {
            this.x = x;
            this.y = y;
        }

        public Point2DServer add(Point2DServer other) {
            return new Point2DServer(x + other.x, y + other.y);
        }

        public Point2DServer multiply(double factor) {
            return new Point2DServer(x * factor, y * factor);
        }

        public double distance(Point2DServer other) {
            double dx = x - other.x;
            double dy = y - other.y;
            return Math.sqrt(dx * dx + dy * dy);
        }

        public double distance(double otherX, double otherY) {
            double dx = x - otherX;
            double dy = y - otherY;
            return Math.sqrt(dx * dx + dy * dy);
        }
    }

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