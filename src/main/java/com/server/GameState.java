package com.server;

import com.shared.GameSnapshot;

public class GameState {

    // TODO: tanques, balas, explosiones, obstaculos, estado de teclas por jugador

    public void startGame(int scenario) {
        // TODO: inicializar partida con el escenario indicado
    }

    public void handleMove(String player, String direction) {
        // TODO: actualizar direccion del jugador
    }

    public void handleAim(String player, double x, double y) {
        // TODO: actualizar posicion del raton del jugador
    }

    public void handleFire(String player) {
        // TODO: crear bala si no supera el maximo
    }

    public void update(double dt) {
        // TODO: mover tanques, balas, comprobar colisiones
    }

    public GameSnapshot toSnapshot() {
        // TODO: generar estado completo para enviar a clientes
        return null;
    }

    public String checkGameOver() {
        // TODO: retorna nombre del ganador o null si sigue
        return null;
    }
}
