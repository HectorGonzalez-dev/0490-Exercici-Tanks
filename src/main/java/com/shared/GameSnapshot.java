package com.shared;

import java.util.List;

import org.json.JSONObject;

public class GameSnapshot {

    public int scenario;
    public List<TankState> tanks;
    public List<BulletState> bullets;
    public List<ExplosionState> explosions;
    public List<WallState> walls;
    public int maxBullets;

    // TODO: constructor, toJSON(), fromJSON()
}
