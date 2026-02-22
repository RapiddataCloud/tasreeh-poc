package com.tasreeh.po.notification_service.ws;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SessionRegistry {

    // Sessions keyed by userId (Keycloak sub)
    private final Map<String, Set<WebSocketSession>> sessionsByUser = new ConcurrentHashMap<>();

    // Sessions keyed by role (e.g., "manager") for broadcast
    private final Map<String, Set<WebSocketSession>> sessionsByRole = new ConcurrentHashMap<>();

    public void add(String userId, WebSocketSession session) {
        sessionsByUser.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet()).add(session);
    }

    public void addByRole(String role, WebSocketSession session) {
        sessionsByRole.computeIfAbsent(role, k -> ConcurrentHashMap.newKeySet()).add(session);
    }

    public void remove(String userId, WebSocketSession session) {
        Set<WebSocketSession> set = sessionsByUser.get(userId);
        if (set != null) {
            set.remove(session);
            if (set.isEmpty()) sessionsByUser.remove(userId);
        }

        // Remove from all role sets
        sessionsByRole.values().forEach(sessions -> sessions.remove(session));
    }

    public Set<WebSocketSession> getSessions(String userId) {
        return sessionsByUser.getOrDefault(userId, Set.of());
    }

    public Set<WebSocketSession> getSessionsByRole(String role) {
        return sessionsByRole.getOrDefault(role, Set.of());
    }

    /** Returns all active sessions across all users (used for graceful shutdown) */
    public Set<WebSocketSession> getAllSessions() {
        Set<WebSocketSession> all = ConcurrentHashMap.newKeySet();
        sessionsByUser.values().forEach(all::addAll);
        return all;
    }
}
