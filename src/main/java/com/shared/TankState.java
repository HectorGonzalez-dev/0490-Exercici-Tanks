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

    /** Constructor vacío. */
    public TankState() {
        this(NO_ID, "", "DODGERBLUE", 0, 0, 0, 0, GameConfig.INITIAL_LIVES, true);
    }

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

    /** Crea una TankState a partir de JSON. */
    public static TankState fromJSON(JSONObject obj) {
        TankState tank = new TankState();
        tank.id = obj.optInt("id", NO_ID);
        tank.name = obj.optString("name", "");
        tank.color = obj.optString("color", "DODGERBLUE");
        tank.x = obj.optDouble("x", 0);
        tank.y = obj.optDouble("y", 0);
        tank.angle = obj.optDouble("angle", 0);
        tank.turretAngle = obj.optDouble("turretAngle", 0);
        tank.lives = obj.optInt("lives", GameConfig.INITIAL_LIVES);
        tank.alive = obj.optBoolean("alive", true);
        return tank;
    }

}
