package com.server;

import org.java_websocket.WebSocket;
import org.json.JSONArray;

import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

final class ClientRegistry {

    private final Map<WebSocket, String> bySocket = new ConcurrentHashMap<>();
    private final Map<String, WebSocket> byName = new ConcurrentHashMap<>();
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
        byName.put(name, socket);
        return name;
    }

    String remove(WebSocket socket) {
        String name = bySocket.remove(socket);
        if (name != null) {
            byName.remove(name);
            giveBack(name);
        }
        return name;
    }

    WebSocket socketByName(String name) {
        return byName.get(name);
    }

    String nameBySocket(WebSocket socket) {
        return bySocket.get(socket);
    }

    JSONArray currentNames() {
        JSONArray arr = new JSONArray();
        for (String n : byName.keySet()) {
            arr.put(n);
        }
        return arr;
    }

    String cleanupDisconnected(WebSocket socket) {
        return remove(socket);
    }

    Map<WebSocket, String> snapshot() {
        return Map.copyOf(bySocket);
    }
}
