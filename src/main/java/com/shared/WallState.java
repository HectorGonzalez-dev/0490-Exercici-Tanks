package com.shared;

import org.json.JSONObject;

public class WallState {

    public static final String DEFAULT_COLOR = "DIMGRAY";

    public double x, y, width, height;
    public String color;

    /**
     * Constructor vacío.
     */
    public WallState() {
        this(0, 0, 0, 0, DEFAULT_COLOR);
    }

    /**
     * Constructor a partir de parametros.
     *
     * @param x      esquina superior izquierda
     * @param y      esquina superior izquierda
     * @param width  ancho en píxeles
     * @param height alto en píxeles
     * @param color  color CSS; si es null o está vacío se usa DEFAULT_COLOR
     */
    public WallState(double x, double y, double width, double height, String color) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.color = (color == null || color.isBlank()) ? DEFAULT_COLOR : color;
    }

    @Override
    public String toString() {
        return this.toJSON().toString();
    }

    /** Convierte el estado de la pared a JSON. */
    public JSONObject toJSON() {
        JSONObject obj = new JSONObject();
        obj.put("x", x);
        obj.put("y", y);
        obj.put("width", width);
        obj.put("height", height);
        obj.put("color", color);
        return obj;
    }

    /** Crea una WallState a partir de JSON. */
    public static WallState fromJSON(JSONObject obj) {
        WallState wall = new WallState();
        wall.x = obj.optDouble("x", 0);
        wall.y = obj.optDouble("y", 0);
        wall.width = obj.optDouble("width", 0);
        wall.height = obj.optDouble("height", 0);
        wall.color = obj.optString("color", DEFAULT_COLOR);
        return wall;
    }

}
