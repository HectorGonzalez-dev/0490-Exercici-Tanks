package com.shared;

import org.json.JSONObject;

public class BulletState {

    public double x, y;
    public double angle;

    /** Constructor vacío. */
    public BulletState() {
        this(0, 0, 0);
    }

    /**
     * Constructor a partir de parametros.
     *
     * @param x     centro X
     * @param y     centro Y
     * @param angle dirección en grados
     */
    public BulletState(double x, double y, double angle) {
        this.x = x;
        this.y = y;
        this.angle = angle;
    }

    @Override
    public String toString() {
        return this.toJSON().toString();
    }

    /** Convierte el estado de la bala a JSON. */
    public JSONObject toJSON() {
        JSONObject obj = new JSONObject();
        obj.put("x", x);
        obj.put("y", y);
        obj.put("angle", angle);
        return obj;
    }

    /** Crea una BulletState a partir de JSON. */
    public static BulletState fromJSON(JSONObject obj) {
        BulletState bullet = new BulletState();
        bullet.x = obj.optDouble("x", 0);
        bullet.y = obj.optDouble("y", 0);
        bullet.angle = obj.optDouble("angle", 0);
        return bullet;
    }
    
}
