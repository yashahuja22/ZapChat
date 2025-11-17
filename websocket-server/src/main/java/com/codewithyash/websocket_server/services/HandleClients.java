package com.codewithyash.websocket_server.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class HandleClients {

    private final Map<String, WebSocketSession> connectedClients = new ConcurrentHashMap<>();

    // Add client
    public void addClient(String username, WebSocketSession session) {
        connectedClients.put(username, session);
        log.info("Client added: {}", username);
    }

    // Remove client
    public void removeClient(String username) {
        connectedClients.remove(username);
        log.info("Client removed: {}", username);
    }

    // Check if client is connected
    public boolean isClientConnected(String username) {
        return connectedClients.containsKey(username);
    }

    // Send message to a client
    public boolean sendDataToClient(String username, String message) {
        try {
            WebSocketSession session = connectedClients.get(username);
            if (session != null && session.isOpen()) {
                session.sendMessage(new TextMessage(message));
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}
