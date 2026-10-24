package com.server;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.json.JSONObject;

import com.shared.GameSnapshot;
import com.shared.Protocol;

public class Main extends WebSocketServer {

    public static final int DEFAULT_PORT = 3000;

    private final ClientRegistry clients;
    private GameState game;
    private final ScheduledExecutorService scheduler;

    // socket -> nombre elegido por el cliente (setName)
    private final Map<WebSocket, String> playerNames = new ConcurrentHashMap<>();
    // socket -> índice de jugador (0 o 1)
    private final Map<WebSocket, Integer> playerIndex = new ConcurrentHashMap<>();
    private int nextPlayerIndex = 0;

    // sockets que han pulsado "volver a jugar" y están esperando una nueva partida
    private final Set<WebSocket> waitingForRestart = ConcurrentHashMap.newKeySet();

    private boolean gameRunning = false;
    private long lastRunNanos = 0;
    // tarea del bucle de partida (~60 fps); se cancela al reiniciar para no duplicarlo
    private ScheduledFuture<?> gameLoopTask = null;

    public Main(InetSocketAddress address) {
        super(address);
        this.clients = new ClientRegistry(List.of("Jugador 1", "Jugador 2"));
        this.game = new GameState();
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        clients.add(conn); // registra la conexión (el nombre de pool se ignora)

        // Asignar índice de jugador (0 = primer cliente, 1 = segundo)
        int idx = nextPlayerIndex;
        nextPlayerIndex++;
        playerIndex.put(conn, idx);

        // Enviar estado de espera a este cliente
        sendWaiting(conn);

        System.out.println("Cliente conectado: " + conn.getRemoteSocketAddress() + " -> jugador " + idx);
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        // Capturar los datos del que se va ANTES de borrarlos de los mapas
        String loserName = playerNames.getOrDefault(conn, "Jugador");
        Integer loserIdx = playerIndex.remove(conn);
        playerNames.remove(conn);
        waitingForRestart.remove(conn);
        clients.remove(conn);
        System.out.println("Cliente desconectado: " + conn.getRemoteSocketAddress());

        if (gameRunning) {
            gameRunning = false;
            // El rival que sigue conectado gana; el que se fue pierde como desconectado
            String winnerName = "?";
            if (loserIdx != null) {
                for (Map.Entry<WebSocket, Integer> e : playerIndex.entrySet()) {
                    if (!e.getValue().equals(loserIdx)) {
                        winnerName = playerNames.getOrDefault(e.getKey(), "Jugador");
                        break;
                    }
                }
            }
            JSONObject go = new JSONObject();
            go.put("type", Protocol.GAME_OVER);
            go.put("winner", winnerName);
            go.put("loser", loserName + " (Desconectado)");
            sendToAll(go.toString());
        }
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        try {
            JSONObject msg = new JSONObject(message);
            String type = msg.optString("type", "");
            Integer idx = playerIndex.get(conn);
            if (idx == null) return;

            String playerKey = (idx == 0) ? "player0" : "player1";

            switch (type) {
                case Protocol.SET_NAME: {
                    String name = msg.optString("name", "Jugador");
                    if (name == null || name.isBlank()) name = "Jugador";
                    playerNames.put(conn, name);

                    // Actualizar nombre del tanque en GameState (para que gameOver muestre el nombre real)
                    // GameState usa nombres por defecto ("Jugador 1"/"Jugador 2"); los sobreescribimos aquí:
                    updateTankName(playerKey, name);

                    // Notificar a ambos clientes quién es quién
                    broadcastWaiting();

                    // Si ya hay 2 jugadores conectados y nombrados, iniciar cuenta atrás
                    if (playerNames.size() >= 2 && !gameRunning) {
                        startCountdown();
                    }
                    break;
                }

                case Protocol.MOVE: {
                    String direction = msg.optString("direction", "");
                    game.handleMove(playerKey, direction);
                    break;
                }

                case Protocol.AIM: {
                    double x = msg.optDouble("x", 0);
                    double y = msg.optDouble("y", 0);
                    game.handleAim(playerKey, x, y);
                    break;
                }

                case Protocol.FIRE: {
                    game.handleFire(playerKey);
                    break;
                }

                case Protocol.PLAY_AGAIN: {
                    // Este jugador vuelve a la sala de espera
                    waitingForRestart.add(conn);

                    if (waitingForRestart.size() >= 2) {
                        // Los dos jugadores han vuelto: nueva cuenta atrás
                        restartGame();
                    } else {
                        // Solo uno espera: avisarle de que aún falta el rival
                        sendWaiting(conn);
                    }
                    break;
                }

                default:
                    // Mensaje desconocido: ignorar
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        ex.printStackTrace();
    }

    @Override
    public void onStart() {
        System.out.println("WebSocket server started on port: " + getPort());
        setConnectionLostTimeout(100);
    }

    public static void main(String[] args) {
        Main server = new Main(new InetSocketAddress(DEFAULT_PORT));
        server.start();
        System.out.println("Server running on port " + DEFAULT_PORT + ". Press Ctrl+C to stop it.");
    }

    /** Nombre del jugador en el índice dado (0 o 1), o "?" si aún no se conoce. */
    private String nameByIndex(int index) {
        for (Map.Entry<WebSocket, Integer> e : playerIndex.entrySet()) {
            if (e.getValue() == index) {
                // Tras acabar la partida, un jugador solo es visible en la sala de
                // espera si también ha pulsado "volver a jugar"; si no, muestra "?".
                if (!waitingForRestart.isEmpty() && !waitingForRestart.contains(e.getKey())) {
                    return "?";
                }
                return playerNames.getOrDefault(e.getKey(), "?");
            }
        }
        return "?";
    }

    /** Envía el mensaje "waiting" a un cliente concreto (con su propio clientName). */
    private void sendWaiting(WebSocket conn) {
        JSONObject msg = new JSONObject();
        msg.put("type", Protocol.WAITING);
        msg.put("clientName", playerNames.getOrDefault(conn, "Jugador"));
        msg.put("player0", nameByIndex(0));
        msg.put("player1", nameByIndex(1));
        sendSafe(conn, msg.toString());
    }

    /** Reenvía "waiting" a todos los clientes conectados (para que vean el nombre del rival). */
    private void broadcastWaiting() {
        for (WebSocket conn : clients.snapshot().keySet()) {
            sendWaiting(conn);
        }
    }

    /** Envía un texto a un socket si está abierto. */
    private void sendSafe(WebSocket conn, String text) {
        if (conn != null && conn.isOpen()) {
            conn.send(text);
        }
    }

    /** Envía un texto a todos los clientes conectados. */
    private void sendToAll(String text) {
        for (WebSocket conn : clients.snapshot().keySet()) {
            sendSafe(conn, text);
        }
    }

    /** Sobreescribe el nombre de un tanque en GameState (sin editar GameState: usamos reflexión vía campos públicos). */
    private void updateTankName(String playerKey, String name) {
        // GameState no expide setter de nombres; mantenemos playerNames como fuente de verdad
        // y usamos esos nombres en waiting/gameOver. Este método queda como punto de extensión.
    }

    private void startCountdown() {
        gameRunning = true; // bloquea para no arrancar dos cuentas atrás
        for (int i = 5; i >= 0; i--) {
            final int value = i;
            scheduler.schedule(() -> {
                JSONObject msg = new JSONObject();
                msg.put("type", Protocol.COUNTDOWN);
                msg.put("value", value);
                sendToAll(msg.toString());
            }, 5 - i, TimeUnit.SECONDS);
        }
        // Al acabar la cuenta atrás, iniciar partida y bucle
        scheduler.schedule(() -> {
            int scenario = 1 + (int)(Math.random() * 3);
            game.startGame(scenario);
            lastRunNanos = System.nanoTime();
            startGameLoop();
        }, 6, TimeUnit.SECONDS);
    }

    private void startGameLoop() {
        if (gameLoopTask != null) {
            gameLoopTask.cancel(false); // evita dos bucles enviando estados a la vez
        }
        long periodMs = 1000 / 60; // ~60 fps
        gameLoopTask = scheduler.scheduleAtFixedRate(() -> {
            if (!gameRunning) return;

            long now = System.nanoTime();
            double dt = (now - lastRunNanos) / 1_000_000_000.0;
            lastRunNanos = now;
            if (dt <= 0 || dt > 0.1) dt = 1.0 / 60.0; // limitar dt tras pausas

            game.update(dt);

            // Enviar snapshot a cada cliente con su youIndex propio
            for (Map.Entry<WebSocket, Integer> e : playerIndex.entrySet()) {
                WebSocket conn = e.getKey();
                int youIndex = e.getValue();
                GameSnapshot snap = game.toSnapshotFor(youIndex);
                sendSafe(conn, snap.toMessage().toString());
            }

            // Comprobar fin de partida (un jugador se quedó sin vidas)
            String winnerPlayerKey = game.checkGameOver();
            if (winnerPlayerKey != null) {
                int winnerIndex = "player0".equals(winnerPlayerKey) ? 0 : 1;
                int loserIndex = 1 - winnerIndex;
                JSONObject go = new JSONObject();
                go.put("type", Protocol.GAME_OVER);
                go.put("winner", nameByIndex(winnerIndex));
                go.put("loser", nameByIndex(loserIndex));
                sendToAll(go.toString());
                gameRunning = false;
            }
        }, 0, periodMs, TimeUnit.MILLISECONDS);
    }

    private void restartGame() {
        gameRunning = false;
        waitingForRestart.clear();
        // Nuevo estado de partida (limpia tanques, balas, explosiones, teclas)
        game = new GameState();
        lastRunNanos = System.nanoTime();
        // Avisar a ambos clientes que vuelven a la espera y reiniciar la cuenta atrás
        broadcastWaiting();
        startCountdown();
        System.out.println("Partida reiniciada (playAgain)");
    }
}
