package com.server;

import java.util.ArrayList;
import java.util.List;

import com.shared.GameConfig;
import com.shared.WallState;

/**
 * Generador de escenarios.
 *
 * Cada escenario es una lista de {@link WallState}: las cuatro paredes exteriores
 * (siempre presentes) más los obstáculos interiores propios de cada mapa.
 *
 * La clase se usa solo en el servidor. El cliente recibe las paredes dentro
 * del {@code GameSnapshot} y no necesita regenerar los escenarios.
 */
public final class Scenarios {

    private Scenarios() { }

    /** Número total de escenarios disponibles. */
    public static final int NUM_SCENARIOS = GameConfig.NUM_SCENARIOS;

    /** Color de las paredes exteriores. */
    private static final String WALL_COLOR = "DIMGRAY";

    /** Color de los obstáculos interiores. */
    private static final String OBSTACLE_COLOR = "LIGHTGRAY";

    /** Ancho y alto del Canvas, en píxeles. */
    private static final double CANVAS_W = GameConfig.CANVAS_WIDTH;
    private static final double CANVAS_H = GameConfig.CANVAS_HEIGHT;
    private static final double WALL_THICKNESS = GameConfig.WALL_THICKNESS;

    /**
     * Devuelve la lista de paredes del escenario indicado.
     *
     * @param id identificador del escenario: 1 = Arena, 2 = Pasillos, 3 = Laberinto
     * @return lista de paredes (incluidas las 4 exteriores)
     * @throws IllegalArgumentException si el id no es 1, 2 ni 3
     */
    public static List<WallState> getScenario(int id) {
        List<WallState> walls = new ArrayList<>();

        // Las 4 paredes exteriores siempre presentes.
        addOuterWalls(walls);

        switch (id) {
            case 1 -> addArenaObstacles(walls);
            case 2 -> addCorridorsObstacles(walls);
            case 3 -> addMazeObstacles(walls);
            default -> throw new IllegalArgumentException(
                    "Escenario no válido: " + id + ". Valores aceptados: 1, 2, 3");
        }

        return walls;
    }

    /** Añade las 4 paredes del borde del escenario. */
    private static void addOuterWalls(List<WallState> walls) {
        walls.add(new WallState(0, 0, CANVAS_W, WALL_THICKNESS, WALL_COLOR));                        // arriba
        walls.add(new WallState(0, CANVAS_H - WALL_THICKNESS, CANVAS_W, WALL_THICKNESS, WALL_COLOR));  // abajo
        walls.add(new WallState(0, 0, WALL_THICKNESS, CANVAS_H, WALL_COLOR));                       // izquierda
        walls.add(new WallState(CANVAS_W - WALL_THICKNESS, 0, WALL_THICKNESS, CANVAS_H, WALL_COLOR)); // derecha
    }

    /**
     * Escenario 1 — Arena.
     * Cuatro obstáculos simétricos al centro, espacios amplios para moverse.
     */
    private static void addArenaObstacles(List<WallState> walls) {
        double w = 40, h = 80;
        walls.add(new WallState(280, 100, w, h, OBSTACLE_COLOR));   // arriba-izquierda
        walls.add(new WallState(480, 100, w, h, OBSTACLE_COLOR));   // arriba-derecha
        walls.add(new WallState(280, 320, w, h, OBSTACLE_COLOR));   // abajo-izquierda
        walls.add(new WallState(480, 320, w, h, OBSTACLE_COLOR));   // abajo-derecha
    }

    /**
     * Escenario 2 — Pasillos.
     * Paredes que crean corredores estrechos.
     */
    private static void addCorridorsObstacles(List<WallState> walls) {
        // Pared vertical izquierda
        walls.add(new WallState(130, 50, 20, 360, OBSTACLE_COLOR));
        // Pared vertical derecha
        walls.add(new WallState(650, 50, 20, 360, OBSTACLE_COLOR));
        // Pared horizontal central
        walls.add(new WallState(230, 240, 340, 20, OBSTACLE_COLOR));
    }

    /**
     * Escenario 3 — Laberinto.
     * Más paredes y varios caminos posibles entre los extremos.
     */
    private static void addMazeObstacles(List<WallState> walls) {
        double w = 60, h = 140;
        walls.add(new WallState(200, 100, w, h, OBSTACLE_COLOR));   // arriba-izquierda
        walls.add(new WallState(540, 100, w, h, OBSTACLE_COLOR));   // arriba-derecha
        walls.add(new WallState(370, 220, 60, 60, OBSTACLE_COLOR)); // centro
        walls.add(new WallState(200, 260, w, h, OBSTACLE_COLOR));   // abajo-izquierda
        walls.add(new WallState(540, 260, w, h, OBSTACLE_COLOR));   // abajo-derecha
    }
}