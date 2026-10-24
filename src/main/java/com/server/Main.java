package com.server;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collections;
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

    // sockets que aún están en la pantalla de resultado (han jugado la partida
    // terminada y aún no han pulsado "volver a jugar"). Los que se conectan después
    // de terminar la partida NO están aquí: están en la sala de espera visible.
    private final Set<WebSocket> onResultScreen = ConcurrentHashMap.newKeySet();

    private boolean gameRunning = false;
    private long lastRunNanos = 0;
    // tarea del bucle de partida (~60 fps); se cancela al reiniciar para no duplicarlo
    private ScheduledFuture<?> gameLoopTask = null;
    // tareas de la cuenta atrás (números COUNTDOWN + arranque de partida); se
    // cancelan si la partida se aborta (p. ej. un jugador se desconecta durante
    // la cuenta atrás) para que no disparen una cuenta atrás muerta sobre los
    // clientes que quedan, lo que les enviaría a una pantalla de juego sin estado
    private final List<ScheduledFuture<?>> countdownTasks = Collections.synchronizedList(new ArrayList<>());

    public Main(InetSocketAddress address) {
        super(address);
        this.clients = new ClientRegistry(List.of("Jugador 1", "Jugador 2"));
        this.game = new GameState();
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        clients.add(conn); // registra la conexión (el nombre de pool se ignora)

        // Reutilizar el índice libre (0 o 1): si un jugador se desconectó, su puesto
        // queda libre para el siguiente cliente. Si no, se asigna el primero vacío.
        Integer idx = null;
        for (int i = 0; i < 2; i++) {
            final int candidate = i;
            boolean taken = playerIndex.containsValue(candidate);
            if (!taken) {
                idx = candidate;
                break;
            }
        }
        if (idx == null) {
            idx = nextPlayerIndex; // ya hay 2 jugadores: no debería ocurrir
            nextPlayerIndex++;
        }
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
        onResultScreen.remove(conn);
        clients.remove(conn);
        System.out.println("Cliente desconectado: " + conn.getRemoteSocketAddress());

        if (gameRunning) {
            gameRunning = false;
            // Si la desconexión ocurre durante la cuenta atrás, quedan mensajes
            // COUNTDOWN y el arranque de partida programados: cancelar todo para
            // que no disparen una partida muerta sobre los clientes que quedan.
            cancelCountdownTasks();
            // El que se queda ve la pantalla de resultado: marcarlo en onResultScreen
            // para que su nombre solo sea visible cuando pulse "volver a jugar".
            onResultScreen.addAll(playerIndex.keySet());

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

        // Sin uno de los dos jugadores la partida ya no puede continuar: cancelar el
        // bucle de juego para que no se reanude con el estado muerto al entrar un nuevo cliente.
        if (gameLoopTask != null) {
            gameLoopTask.cancel(false);
            gameLoopTask = null;
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

                    // Si ya hay 2 jugadores conectados y nombrados, no hay partida en curso
                    // y NINGUNO está todavía en la pantalla de resultado, iniciar cuenta atrás.
                    // Sin el guard, la entrada de un cliente nuevo reanudaría la partida
                    // anterior; y sin el check de onResultScreen, se forzaría el inicio
                    // aunque al jugador que espera le faltara pulsar "volver a jugar".
                    if (playerNames.size() >= 2 && !gameRunning && onResultScreen.isEmpty()) {
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
                    if (gameRunning) break; // sin partida terminada no hay "volver a jugar"

                    // Este jugador sale de la pantalla de resultado y vuelve a la sala de espera
                    onResultScreen.remove(conn);

                    // Reiniciar cuando todos los conectados están ya en la sala de espera
                    // (si falta un segundo jugador, startCountdown no debe arrancar aún)
                    if (onResultScreen.isEmpty() && playerIndex.size() >= 2) {
                        restartGame();
                    } else {
                        // Aún falta alguien (o solo hay un jugador): avisarle de que espere
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
                // espera si NO está en la pantalla de resultado (o sea, si ya pulsó
                // "volver a jugar" o entró después de terminar la partida); si no, "?".
                if (onResultScreen.contains(e.getKey())) {
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

    /** Sobreescribe el nombre de un tanque en GameState con el nombre elegido por el cliente. */
    private void updateTankName(String playerKey, String name) {
        game.setTankName(playerKey, name);
    }

    private void startCountdown() {
        gameRunning = true; // bloquea para no arrancar dos cuentas atrás
        onResultScreen.clear(); // todos dejan la pantalla de resultado: empiezan a jugar
        cancelCountdownTasks(); // descarta tareas de una cuenta atrás anterior abortada
        for (int i = 5; i >= 0; i--) {
            final int value = i;
            countdownTasks.add(scheduler.schedule(() -> {
                if (!gameRunning) return; // la cuenta atrás se abortó (jugador desconectado)
                JSONObject msg = new JSONObject();
                msg.put("type", Protocol.COUNTDOWN);
                msg.put("value", value);
                sendToAll(msg.toString());
            }, 5 - i, TimeUnit.SECONDS));
        }
        // Al acabar la cuenta atrás, iniciar partida y bucle
        countdownTasks.add(scheduler.schedule(() -> {
            if (!gameRunning) return; // sin partida en curso: no iniciar sobre un estado muerto
            int scenario = 1 + (int)(Math.random() * 3);
            game.startGame(scenario);
            // startGame crea los tanques con nombres por defecto ("Jugador 1/2"):
            // sustituirlos por los nombres reales que cada cliente eligió con setName.
            for (Map.Entry<WebSocket, Integer> e : playerIndex.entrySet()) {
                String name = playerNames.get(e.getKey());
                if (name != null) {
                    game.setTankName(e.getValue() == 0 ? "player0" : "player1", name);
                }
            }
            lastRunNanos = System.nanoTime();
            startGameLoop();
        }, 6, TimeUnit.SECONDS));
    }

    /** Cancela y limpia las tareas programadas de la cuenta atrás. */
    private void cancelCountdownTasks() {
        for (ScheduledFuture<?> t : countdownTasks) {
            t.cancel(false);
        }
        countdownTasks.clear();
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
                // A partir de ahora ambos están en la pantalla de resultado: sus nombres
                // solo volverán a ser visibles cuando pulsen "volver a jugar".
                onResultScreen.addAll(playerIndex.keySet());
                gameRunning = false;
            }
        }, 0, periodMs, TimeUnit.MILLISECONDS);
    }

    private void restartGame() {
        gameRunning = false;
        onResultScreen.clear();
        // Nuevo estado de partida (limpia tanques, balas, explosiones, teclas)
        game = new GameState();
        lastRunNanos = System.nanoTime();
        // Avisar a ambos clientes que vuelven a la espera y reiniciar la cuenta atrás
        broadcastWaiting();
        startCountdown();
        System.out.println("Partida reiniciada (playAgain)");
    }
}
