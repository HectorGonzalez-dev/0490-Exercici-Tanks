package com.server;

public final class ServerCollisions {

    private ServerCollisions() { }

    // Dos círculos se tocan si la distancia entre centros < suma de radios.
    public static boolean circlesOverlap(Point2DServer centerA, double radiusA,
                                         Point2DServer centerB, double radiusB) {
        return centerA.distance(centerB) < radiusA + radiusB;
    }

    // Un círculo toca un rectángulo (esquina superior izquierda x,y, ancho w, alto h).
    public static boolean circleOverlapsRect(Point2DServer center, double radius,
                                             double rectX, double rectY, double rectW, double rectH) {
        double nearestX = clamp(center.x, rectX, rectX + rectW);
        double nearestY = clamp(center.y, rectY, rectY + rectH);
        return center.distance(nearestX, nearestY) < radius;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(value, max));
    }
}