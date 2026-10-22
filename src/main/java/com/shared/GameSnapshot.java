package com.shared;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

public class GameSnapshot {

    /** Escenario en juego: 1 = Arena, 2 = Pasillos, 3 = Laberinto. */
    public int scenario;

    public List<TankState> tanks;
    public List<BulletState> bullets;
    public List<ExplosionState> explosions;
    public List<WallState> walls;

    /** Balas activas por tanque: activeBullets[i] corresponde a tanks.get(i). */
    public int[] activeBullets;

    /** Máximo de balas simultáneas por jugador. */
    public int maxBullets;

    /** Índice del tanque propio dentro de tanks; -1 si el cliente no lo sabe. */
    public int youIndex;

    /** Constructor vacío: listas vacías y valores neutros. */
    public GameSnapshot() {
        this.scenario = 1;
        this.tanks = new ArrayList<>();
        this.bullets = new ArrayList<>();
        this.explosions = new ArrayList<>();
        this.walls = new ArrayList<>();
        this.activeBullets = new int[0];
        this.maxBullets = GameConfig.MAX_BULLETS;
        this.youIndex = -1;
    }

    /**
     * Constructor a partir de JSON.
     *
     * @param obj JSON a convertir
     */
    public GameSnapshot(JSONObject obj) {
        this.scenario = obj.optInt("scenario", 1);
        this.maxBullets = obj.optInt("maxBullets", GameConfig.MAX_BULLETS);
        this.youIndex = obj.optInt("youIndex", -1);

        // Tanques
        this.tanks = new ArrayList<>();
        JSONArray arrTanks = obj.optJSONArray("tanks");
        if (arrTanks != null) {
            for (int i = 0; i < arrTanks.length(); i++) {
                JSONObject o = arrTanks.optJSONObject(i);
                if (o != null) this.tanks.add(new TankState(o));
            }
        }

        // Balas
        this.bullets = new ArrayList<>();
        JSONArray arrBullets = obj.optJSONArray("bullets");
        if (arrBullets != null) {
            for (int i = 0; i < arrBullets.length(); i++) {
                JSONObject o = arrBullets.optJSONObject(i);
                if (o != null) this.bullets.add(new BulletState(o));
            }
        }

        // Explosiones
        this.explosions = new ArrayList<>();
        JSONArray arrExplosions = obj.optJSONArray("explosions");
        if (arrExplosions != null) {
            for (int i = 0; i < arrExplosions.length(); i++) {
                JSONObject o = arrExplosions.optJSONObject(i);
                if (o != null) this.explosions.add(new ExplosionState(o));
            }
        }

        // Paredes
        this.walls = new ArrayList<>();
        JSONArray arrWalls = obj.optJSONArray("walls");
        if (arrWalls != null) {
            for (int i = 0; i < arrWalls.length(); i++) {
                JSONObject o = arrWalls.optJSONObject(i);
                if (o != null) this.walls.add(new WallState(o));
            }
        }

        // Balas activas por tanque
        JSONArray arrActive = obj.optJSONArray("activeBullets");
        this.activeBullets = new int[arrActive != null ? arrActive.length() : 0];

        if (arrActive != null) {
            for (int i = 0; i < arrActive.length(); i++) {
                this.activeBullets[i] = arrActive.optInt(i, 0);
            }
        }
    }

    @Override
    public String toString() {
        return this.toJSON().toString();
    }

    /** Convierte el snapshot completo a JSON. */
    public JSONObject toJSON() {
        JSONObject obj = new JSONObject();

        obj.put("scenario", scenario);
        obj.put("maxBullets", maxBullets);
        obj.put("youIndex", youIndex);

        // Listas de objetos
        JSONArray arrTanks = new JSONArray();
        for (TankState tank : tanks) {
            arrTanks.put(tank.toJSON());
        }
        obj.put("tanks", arrTanks);

        JSONArray arrBullets = new JSONArray();
        for (BulletState bullet : bullets) {
            arrBullets.put(bullet.toJSON());
        }
        obj.put("bullets", arrBullets);

        JSONArray arrExplosions = new JSONArray();
        for (ExplosionState explosion : explosions) {
            arrExplosions.put(explosion.toJSON());
        }
        obj.put("explosions", arrExplosions);

        JSONArray arrWalls = new JSONArray();
        for (WallState wall : walls) {
            arrWalls.put(wall.toJSON());
        }
        obj.put("walls", arrWalls);

        JSONArray arrActive = new JSONArray();
        for (int n : activeBullets) {
            arrActive.put(n);
        }
        obj.put("activeBullets", arrActive);

        return obj;
    }

    /**
     * Envuelve el snapshot en un mensaje del protocolo ("gameState").
     */
    public JSONObject toMessage() {
        JSONObject obj = toJSON();
        obj.put("type", Protocol.GAME_STATE);
        return obj;
    }

    // ---- Ayudantes de lectura para el cliente ----

    /**
     * Devuelve el tanque del índice indicado, o null si no existe.
     *
     * @param index índice dentro de la lista tanks
     * @return el tanque, o null
     */
    public TankState getTank(int index) {
        if (index < 0 || index >= tanks.size()) return null;
        return tanks.get(index);
    }

    /**
     * Devuelve mi tanque (el del índice youIndex), o null si no se conoce.
     *
     * @return el tanque propio, o null
     */
    public TankState getMyTank() {
        return getTank(youIndex);
    }

    /**
     * Número de balas activas del jugador indicado.
     *
     * @param index índice dentro de la lista tanks
     * @return las balas activas, o 0 si el índice no existe
     */
    public int getActiveBullets(int index) {
        if (index < 0 || index >= activeBullets.length) return 0;
        return activeBullets[index];
    }
}
