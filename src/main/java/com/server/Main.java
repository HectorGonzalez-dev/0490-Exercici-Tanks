package com.server;

import java.net.InetSocketAddress;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

public class Main extends WebSocketServer {

    public static final int DEFAULT_PORT = 3000;

    public Main(InetSocketAddress address) {
        super(address);
        // TODO: inicializar ClientRegistry, GameState, ticker
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        // TODO: registrar client, enviar waiting
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        // TODO: eliminar client
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        // TODO: parsear JSON, delegar a GameState
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        ex.printStackTrace();
    }

    @Override
    public void onStart() {
        System.out.println("WebSocket server started on port: " + getPort());
        setConnectionLostTimeout(100);
        // TODO: iniciar ticker
    }

    public static void main(String[] args) {
        Main server = new Main(new InetSocketAddress(DEFAULT_PORT));
        server.start();
        System.out.println("Server running on port " + DEFAULT_PORT + ". Press Ctrl+C to stop it.");
    }
}
