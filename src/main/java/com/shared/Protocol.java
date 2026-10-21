package com.shared;

public final class Protocol {

    private Protocol() { }

    // Cliente -> Servidor
    public static final String SET_NAME = "setName";
    public static final String MOVE = "move";
    public static final String AIM = "aim";
    public static final String FIRE = "fire";
    public static final String PLAY_AGAIN = "playAgain";

    // Servidor -> Cliente
    public static final String WAITING = "waiting";
    public static final String COUNTDOWN = "countdown";
    public static final String GAME_STATE = "gameState";
    public static final String GAME_OVER = "gameOver";
}
