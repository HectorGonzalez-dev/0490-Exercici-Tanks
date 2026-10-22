package com.shared;

import org.json.JSONObject;

public class ExplosionState {

    public double x, y;
    public double radius;
    public double progress;

    /**
     * Constructor a partir de parametros.
     *
     * @param x        centro X
     * @param y        centro Y
     * @param radius   radio máximo en píxeles
     * @param progress avance de 0.0 a 1.0
     */
    public ExplosionState(double x, double y, double radius, double progress) {
        this.x = x;
        this.y = y;
        this.radius = radius;
        this.progress = progress;
    }

    /**
     * Constructor a partir de JSON.
     *
     * @param obj JSON a convertir
     */
    public ExplosionState(JSONObject obj) {
        this(
            obj.optDouble("x", 0),
            obj.optDouble("y", 0),
            obj.optDouble("radius", 0),
            obj.optDouble("progress", 0)
        );
    }

    @Override
    public String toString() {
        return this.toJSON().toString();
    }

    /** Convierte el estado de la explosión a JSON. */
    public JSONObject toJSON() {
        JSONObject obj = new JSONObject();
        obj.put("x", x);
        obj.put("y", y);
        obj.put("radius", radius);
        obj.put("progress", progress);
        return obj;
    }

}
