package com.server;

/** Punto 2D puro (sin JavaFX) para cálculos del servidor. */
public final class Point2DServer {

    public final double x;
    public final double y;

    public Point2DServer(double x, double y) {
        this.x = x;
        this.y = y;
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