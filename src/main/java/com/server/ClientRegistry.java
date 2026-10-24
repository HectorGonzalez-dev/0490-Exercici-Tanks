package com.server;

import org.java_websocket.WebSocket;

import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

final class ClientRegistry {

    private final Map<WebSocket, String> bySocket = new ConcurrentHashMap<>();
    private final Queue<String> pool = new ConcurrentLinkedQueue<>();
    private final List<String> seedNames;

    ClientRegistry(List<String> seedNames) {
        this.seedNames = seedNames;
        resetPool();
    }

    private synchronized void resetPool() {
        pool.clear();
        pool.addAll(seedNames);
    }

    private String takeOrRecycle() {
        String name = pool.poll();
        if (name == null) {
            resetPool();
            name = pool.poll();
        }
        return name;
    }

    private void giveBack(String name) {
        if (name != null) {
            pool.offer(name);
        }
    }

    String add(WebSocket socket) {
        String name = takeOrRecycle();
        bySocket.put(socket, name);
        return name;
    }

    String remove(WebSocket socket) {
        String name = bySocket.remove(socket);
        if (name != null) {
            giveBack(name);
        }
        return name;
    }

    Map<WebSocket, String> snapshot() {
        return Map.copyOf(bySocket);
    }
}
