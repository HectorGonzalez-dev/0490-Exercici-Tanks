package com.shared;

import org.json.JSONObject;

public class TankState {

    public static final int NO_ID = -1;

    /** Identificador estable del jugador (0 y 1). */
    public int id;

    /** Nombre elegido por el jugador en la vista de configuración. */
    public String name;

    public String color;
    public double x, y;
    public double angle;
    public double turretAngle;
    public int lives;
    public boolean alive;

    /**
     * Constructor a partir de parametros.
     *
     * @param id          identificador del jugador (0 o 1)
     * @param name        nombre del jugador
     * @param color       color CSS del tanque
     * @param x           centro X
     * @param y           centro Y
     * @param angle       orientación del cuerpo en grados
     * @param turretAngle orientación de la torreta en grados
     * @param lives       vidas restantes
     * @param alive       si el tanque sigue en juego
     */
    public TankState(int id, String name, String color,
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
    }

    /**
     * Constructor a partir de JSON.
     *
     * @param obj JSON a convertir
     */
    public TankState(JSONObject obj) {
        this(
            obj.optInt("id", NO_ID),
            obj.optString("name", ""),
            obj.optString("color", "DODGERBLUE"),
            obj.optDouble("x", 0),
            obj.optDouble("y", 0),
            obj.optDouble("angle", 0),
            obj.optDouble("turretAngle", 0),
            obj.optInt("lives", GameConfig.INITIAL_LIVES),
            obj.optBoolean("alive", true)
        );
    }

    @Override
    public String toString() {
        return this.toJSON().toString();
    }

    /** Convierte el estado del tanque a JSON. */
    public JSONObject toJSON() {
        JSONObject obj = new JSONObject();
        obj.put("id", id);
        obj.put("name", name);
        obj.put("color", color);
        obj.put("x", x);
        obj.put("y", y);
        obj.put("angle", angle);
        obj.put("turretAngle", turretAngle);
        obj.put("lives", lives);
        obj.put("alive", alive);
        return obj;
    }

    // TODO: constructor, toJSON(), fromJSON()
}
