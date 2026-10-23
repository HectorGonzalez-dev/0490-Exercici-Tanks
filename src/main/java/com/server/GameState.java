package com.server;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.shared.BulletState;
import com.shared.ExplosionState;
import com.shared.GameConfig;
import com.shared.GameSnapshot;
import com.shared.TankState;
import com.shared.WallState;

public class GameState {

    private int scenario;
    private boolean gameRunning = false;

    /** Tanques por nombre de jugador. */
    private final Map<String, ServerTank> tanks = new HashMap<>();

    /** Direcciones activas por jugador (conjunto de "FORWARD", "BACKWARD", "LEFT", "RIGHT"). */
    private final Map<String, Set<String>> pressedKeys = new ConcurrentHashMap<>();

    /** Posición del ratón por nombre de jugador. */
    private final Map<String, double[]> mousePositions = new ConcurrentHashMap<>();

    private final List<ServerBullet> bullets = new ArrayList<>();
    private final List<ServerExplosion> explosions = new ArrayList<>();
    private final List<WallState> walls = new ArrayList<>();

    private long lastRunNanos = 0;

    public void startGame(int scenario) {
        // 1. Cargar paredes del escenario
        this.scenario = scenario;
        this.gameRunning = true;

        // 2. Limpiar objetos de la partida anterior
        bullets.clear();
        explosions.clear();
        pressedKeys.clear();
        mousePositions.clear();

        // 3. Crear los dos tanques en posiciones iniciales
        tanks.clear();
        tanks.put("player0", new ServerTank(
            "0", "Jugador 1", "DODGERBLUE",
            90, 250, 0, 0,
            GameConfig.INITIAL_LIVES, true));
        tanks.put("player1", new ServerTank(
            "1", "Jugador 2", "DARKGOLDENROD",
            710, 250, 180, 180,
            GameConfig.INITIAL_LIVES, true));

        // Inicializar conjuntos de teclas vacíos para ambos
        pressedKeys.put("player0", new HashSet<>());
        pressedKeys.put("player1", new HashSet<>());
        mousePositions.put("player0", new double[]{710, 250});
        mousePositions.put("player1", new double[]{90, 250});

        lastRunNanos = System.nanoTime();
    }

    public void handleMove(String player, String direction) {
        if (!gameRunning || !tanks.containsKey(player)) return;

        Set<String> keys = pressedKeys.computeIfAbsent(player, k -> new HashSet<>());

        switch (direction) {
            case "FORWARD": keys.add("FORWARD"); break;
            case "BACKWARD": keys.add("BACKWARD"); break;
            case "LEFT": keys.add("LEFT"); break;
            case "RIGHT": keys.add("RIGHT"); break;
            case "STOP": keys.clear(); break;
            default: // ignorar direcciones desconocidas
        }
    }

    public void handleAim(String player, double x, double y) {
        if (!gameRunning || !tanks.containsKey(player)) return;
        mousePositions.put(player, new double[]{x, y});
    }

    public void handleFire(String player) {
        if (!gameRunning || !tanks.containsKey(player)) return;

        ServerTank tank = tanks.get(player);
        if (!tank.alive) return;

        // Contar balas activas de este jugador
        int activeBullets = 0;
        for (ServerBullet b : bullets) {
            if (b.owner.equals(player)) activeBullets++;
        }

        if (activeBullets >= GameConfig.MAX_BULLETS) return;

        // Apuntar hacia el ratón antes de disparar
        double[] mouse = mousePositions.getOrDefault(player,
            new double[]{tank.x + 100, tank.y});
        tank.pointAt(mouse[0], mouse[1]);

        // Crear la bala en la posición del tanque
        bullets.add(new ServerBullet(player, tank.x, tank.y, tank.turretAngle));
    }

    public void update(double dt) {
        if (dt <= 0) return;

        double stepDt = dt / GameConfig.PHYSICS_PRECISION;
        for (int step = 0; step < GameConfig.PHYSICS_PRECISION; step++) {
            updateTanks(stepDt);
            updateBullets(stepDt);
        }

        updateExplosions(dt);

        // Apuntar torretas hacia el ratón desde la posición final de cada tanque
        for (ServerTank tank : tanks.values()) {
            if (!tank.alive) continue;
            double[] mouse = mousePositions.getOrDefault(
                tank.id.equals("0") ? "player0" : "player1",
                new double[]{tank.x + 100, tank.y});
            tank.pointAt(mouse[0], mouse[1]);
        }
    }

    /**
     * Crea un snapshot para un cliente concreto.
     *
     * @param youIndex índice del tanque propio dentro de la lista tanks (0 o 1)
     * @return snapshot listo para enviar al cliente
     */
    public GameSnapshot toSnapshotFor(int youIndex) {
        GameSnapshot snapshot = new GameSnapshot();
        snapshot.scenario = scenario;
        snapshot.maxBullets = GameConfig.MAX_BULLETS;
        snapshot.youIndex = youIndex;

        // Tanques
        ServerTank tank0 = tanks.get("player0");
        ServerTank tank1 = tanks.get("player1");
        if (tank0 != null) snapshot.tanks.add(toTankState(tank0));
        if (tank1 != null) snapshot.tanks.add(toTankState(tank1));

        // Balas
        for (ServerBullet bullet : bullets) {
            snapshot.bullets.add(new BulletState(bullet.x, bullet.y, bullet.angle));
        }

        // Explosiones: progreso calculado por el servidor
        for (ServerExplosion explosion : explosions) {
            snapshot.explosions.add(new ExplosionState(
                explosion.x, explosion.y, explosion.radius, explosion.getProgress()));
        }

        // Paredes del escenario
        snapshot.walls.addAll(walls);

        // Balas activas por jugador
        int[] activeBullets = new int[snapshot.tanks.size()];
        int idx = 0;
        for (ServerTank tank : tanks.values()) {
            int count = 0;
            for (ServerBullet bullet : bullets) {
                if (bullet.owner.equals(tank.id.equals("0") ? "player0" : "player1")) count++;
            }
            activeBullets[idx++] = count;
        }
        snapshot.activeBullets = activeBullets;

        return snapshot;
    }

    private TankState toTankState(ServerTank tank) {
        return new TankState(
            Integer.parseInt(tank.id),
            tank.name,
            tank.color,
            tank.x, tank.y,
            tank.angle,
            tank.turretAngle,
            tank.lives,
            tank.alive);
    }

    public String checkGameOver() {
        List<String> alivePlayers = new ArrayList<>();
        for (ServerTank tank : tanks.values()) {
            if (tank.alive) alivePlayers.add(tank.name);
        }

        if (alivePlayers.size() <= 1) {
            if (alivePlayers.isEmpty()) return null; // Ambos murieron
            return alivePlayers.get(0); // El único sobreviviente es el ganador
        }
        return null;
    }

    private void updateTanks(double dt) {
        for (ServerTank tank : tanks.values()) {
            if (!tank.alive) continue;

            // Calcular dirección (dx, dy) a partir de las teclas activas
            Set<String> keys = pressedKeys.getOrDefault(tank.id.equals("0") ? "player0" : "player1",
                Collections.emptySet());
            double dx = 0, dy = 0;
            if (keys.contains("LEFT"))  dx -= 1;
            if (keys.contains("RIGHT")) dx += 1;
            if (keys.contains("FORWARD")) dy -= 1;
            if (keys.contains("BACKWARD")) dy += 1;

            tank.updateMovement(dx, dy, GameConfig.TANK_SPEED);

            Point2DServer current = new Point2DServer(tank.x, tank.y);
            Point2DServer next = tank.nextPosition(dt);

            // Si el movimiento completo choca, probar solo X o solo Y (deslizamiento)
            if (!isTankBlocked(next, tank)) {
                tank.x = next.x;
                tank.y = next.y;
            } else if (!isTankBlocked(new Point2DServer(next.x, current.y), tank)) {
                tank.x = next.x;
            } else if (!isTankBlocked(new Point2DServer(current.x, next.y), tank)) {
                tank.y = next.y;
            }
            // Si todo está bloqueado, el tanque se queda quieto
        }
    }

    private boolean isTankBlocked(Point2DServer position, ServerTank tank) {
        // Colisión con paredes y obstáculos
        for (WallState wall : walls) {
            if (ServerCollisions.circleOverlapsRect(position, ServerTank.RADIUS,
                wall.x, wall.y, wall.width, wall.height)) {
                return true;
            }
        }

        // Colisión con el otro tanque
        for (ServerTank other : tanks.values()) {
            if (other == tank || !other.alive) continue;
            if (ServerCollisions.circlesOverlap(position, ServerTank.RADIUS,
                new Point2DServer(other.x, other.y), ServerTank.RADIUS)) {
                return true;
            }
        }

        return false;
    }

    private void updateBullets(double dt) {
        List<ServerBullet> bulletsToRemove = new ArrayList<>();

        for (ServerBullet bullet : bullets) {
            Point2DServer current = new Point2DServer(bullet.x, bullet.y);
            Point2DServer next = bullet.nextPosition(dt);

            // 1. Pared u obstáculo: primer golpe rebota, segundo desaparece.
            if (touchesWall(next, bullet)) {
                if (bullet.bounced) {
                    bulletsToRemove.add(bullet);
                    addExplosion(current.x, current.y, 20);
                } else {
                    boolean invertX = touchesWall(
                        new Point2DServer(next.x, current.y), bullet);
                    boolean invertY = touchesWall(
                        new Point2DServer(current.x, next.y), bullet);
                    if (!invertX && !invertY) {
                        // Tocó una esquina: rebotar en ambas direcciones
                        invertX = true;
                        invertY = true;
                    }
                    bullet.bounce(invertX, invertY);
                }
                continue; // La bala no avanza este paso
            }
            bullet.x = next.x;
            bullet.y = next.y;

            // 2. Tanques: el enemigo siempre; el suyo solo si ya rebotó.
            for (ServerTank target : tanks.values()) {
                if (!target.alive) continue;
                boolean isOwner = target.id.equals(bullet.owner.charAt(bullet.owner.length() - 1) + "");
                boolean canDamage = !isOwner || bullet.bounced;

                if (canDamage && ServerCollisions.circlesOverlap(
                    new Point2DServer(bullet.x, bullet.y), ServerBullet.RADIUS,
                    new Point2DServer(target.x, target.y), ServerTank.RADIUS)) {

                    target.destroy();
                    bulletsToRemove.add(bullet);
                    addExplosion(target.x, target.y, 60); // Explosión grande al destruir tanque
                    break;
                }
            }
        }

        // 3. Dos balas que se tocan desaparecen ambas.
        for (int i = 0; i < bullets.size(); i++) {
            for (int j = i + 1; j < bullets.size(); j++) {
                ServerBullet a = bullets.get(i);
                ServerBullet b = bullets.get(j);
                if (ServerCollisions.circlesOverlap(
                    new Point2DServer(a.x, a.y), ServerBullet.RADIUS,
                    new Point2DServer(b.x, b.y), ServerBullet.RADIUS)) {
                    bulletsToRemove.add(a);
                    bulletsToRemove.add(b);
                    addExplosion(
                        (a.x + b.x) / 2, (a.y + b.y) / 2, 20);
                }
            }
        }

        bullets.removeAll(bulletsToRemove);
    }

    private boolean touchesWall(Point2DServer position, ServerBullet bullet) {
        for (WallState wall : walls) {
            if (ServerCollisions.circleOverlapsRect(position, ServerBullet.RADIUS,
                wall.x, wall.y, wall.width, wall.height)) {
                return true;
            }
        }
        return false;
    }

    private void updateExplosions(double dt) {
        for (ServerExplosion explosion : explosions) {
            explosion.update(dt);
        }
        explosions.removeIf(ServerExplosion::isFinished);
    }

    private void addExplosion(double x, double y, double radius) {
        explosions.add(new ServerExplosion(x, y, radius, GameConfig.EXPLOSION_DURATION));
    }
}
