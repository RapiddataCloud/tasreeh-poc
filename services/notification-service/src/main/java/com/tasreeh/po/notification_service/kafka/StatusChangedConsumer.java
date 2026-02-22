package com.tasreeh.po.notification_service.kafka;

import com.tasreeh.po.notification_service.events.OrderStatusChangedEvent;
import com.tasreeh.po.notification_service.ws.EventBuffer;
import com.tasreeh.po.notification_service.ws.SessionRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.DltStrategy;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.HashSet;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class StatusChangedConsumer {

    private final SessionRegistry registry;
    private final EventBuffer eventBuffer;

    @RetryableTopic(
            attempts = "3",
            backoff = @Backoff(delay = 1000, multiplier = 2),
            dltStrategy = DltStrategy.ALWAYS_RETRY_ON_ERROR,
            autoCreateTopics = "true"
    )
    @KafkaListener(topics = "po.order.status-changed")
    public void consume(OrderStatusChangedEvent event) {
        log.info("========================================================");
        log.info("[NOTIFICATION-SERVICE] << RECEIVED from Kafka topic: po.order.status-changed");
        log.info("[NOTIFICATION-SERVICE]   orderId    = {}", event.getOrderId());
        log.info("[NOTIFICATION-SERVICE]   userId     = {}", event.getUserId());
        log.info("[NOTIFICATION-SERVICE]   {} -> {}", event.getPreviousStatus(), event.getNewStatus());
        log.info("[NOTIFICATION-SERVICE]   reason     = {}", event.getReason());
        log.info("========================================================");

        try {
            // Build JSON payload manually to avoid ObjectMapper issues with Instant
            String payload = String.format(
                "{\"orderId\":\"%s\",\"newStatus\":\"%s\",\"reason\":\"%s\",\"timestamp\":\"%s\"}",
                event.getOrderId(),
                event.getNewStatus(),
                event.getReason() != null ? event.getReason().replace("\"", "'") : "",
                event.getTimestamp() != null ? event.getTimestamp().toString() : ""
            );

            TextMessage message = new TextMessage(payload);

            // Collect all sessions to notify (avoid duplicates)
            Set<WebSocketSession> targetSessions = new HashSet<>();

            // 1. Push to the order submitter (by userId)
            String userId = event.getUserId();
            Set<WebSocketSession> userSessions = registry.getSessions(userId);
            targetSessions.addAll(userSessions);
            log.info("[NOTIFICATION-SERVICE] Found {} session(s) for userId={}", userSessions.size(), userId);

            // 2. Broadcast to all connected managers
            Set<WebSocketSession> managerSessions = registry.getSessionsByRole("manager");
            targetSessions.addAll(managerSessions);
            log.info("[NOTIFICATION-SERVICE] Found {} session(s) for role=manager", managerSessions.size());

            log.info("[NOTIFICATION-SERVICE] Total unique sessions to notify: {}", targetSessions.size());

            int sent = 0;
            int closed = 0;
            for (WebSocketSession session : targetSessions) {
                if (session.isOpen()) {
                    try {
                        session.sendMessage(message);
                        sent++;
                        log.info("[NOTIFICATION-SERVICE] >> Sent to session {}", session.getId());
                    } catch (Exception e) {
                        log.error("[NOTIFICATION-SERVICE] Failed to send to session {}: {}", session.getId(), e.getMessage());
                    }
                } else {
                    closed++;
                    log.warn("[NOTIFICATION-SERVICE] Session {} is closed, skipping", session.getId());
                }
            }

            log.info("========================================================");
            log.info("[NOTIFICATION-SERVICE] >> WebSocket push complete: {} sent, {} skipped (closed)", sent, closed);
            log.info("[NOTIFICATION-SERVICE]   payload = {}", payload);
            log.info("========================================================");

            // Store in buffer for reconnection recovery
            eventBuffer.store(event, payload);

        } catch (Exception e) {
            log.error("[NOTIFICATION-SERVICE] ❌ FAILED to push WS message", e);
        }
    }

    @DltHandler
    public void handleDlt(OrderStatusChangedEvent event) {
        log.error("========================================================");
        log.error("[NOTIFICATION-SERVICE] ❌ DLT — Failed after all retries");
        log.error("[NOTIFICATION-SERVICE]   orderId = {}", event.getOrderId());
        log.error("[NOTIFICATION-SERVICE]   userId  = {}", event.getUserId());
        log.error("[NOTIFICATION-SERVICE]   status  = {} -> {}", event.getPreviousStatus(), event.getNewStatus());
        log.error("[NOTIFICATION-SERVICE]   Event sent to: po.order.status-changed-dlt");
        log.error("========================================================");
    }
}
