package com.server;

import com.shared.GameConfig;

public class ServerExplosion {

    public double x;
    public double y;
    public double radius;       // Radio máximo
    public double remaining;    // Tiempo restante en segundos

    public ServerExplosion(double x, double y, double radius, double duration) {
        this.x = x;
        this.y = y;
        this.radius = radius;
        this.remaining = duration;
    }

    public void update(double dt) {
        remaining -= dt;
    }

    public boolean isFinished() {
        return remaining <= 0;
    }

    /** Progreso de 0.0 (empieza) a 1.0 (termina). */
    public double getProgress() {
        return 1.0 - remaining / GameConfig.EXPLOSION_DURATION;
    }
}