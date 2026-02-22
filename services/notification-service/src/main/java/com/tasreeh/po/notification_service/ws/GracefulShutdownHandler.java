package com.tasreeh.po.notification_service.ws;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;

import java.util.Set;

/**
 * Handles graceful shutdown of all WebSocket connections.
 * On application shutdown (SIGTERM), sends GOING_AWAY close status to all connected clients.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GracefulShutdownHandler {

    private final SessionRegistry registry;

    @PreDestroy
    public void onShutdown() {
        Set<WebSocketSession> allSessions = registry.getAllSessions();
        log.info("========================================================");
        log.info("[GRACEFUL-SHUTDOWN] 🛑 Shutting down WebSocket connections");
        log.info("[GRACEFUL-SHUTDOWN]   Active sessions: {}", allSessions.size());
        log.info("========================================================");

        int closed = 0;
        for (WebSocketSession session : allSessions) {
            try {
                if (session.isOpen()) {
                    session.close(new CloseStatus(1001, "Server shutting down"));
                    closed++;
                }
            } catch (Exception e) {
                log.warn("[GRACEFUL-SHUTDOWN] Failed to close session {}: {}", session.getId(), e.getMessage());
            }
        }

        log.info("[GRACEFUL-SHUTDOWN] ✅ Closed {} WebSocket session(s)", closed);
    }
}
