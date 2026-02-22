package com.tasreeh.po.notification_service.ws;

import com.tasreeh.po.notification_service.events.OrderStatusChangedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Circular buffer storing recent status-changed events for WebSocket reconnection recovery.
 * When a client reconnects with a lastEventTime, events since that time are replayed.
 */
@Slf4j
@Component
public class EventBuffer {

    private static final int MAX_EVENTS = 500;
    private final ConcurrentLinkedDeque<BufferedEvent> buffer = new ConcurrentLinkedDeque<>();

    public void store(OrderStatusChangedEvent event, String payload) {
        Instant timestamp = event.getTimestamp() != null ? event.getTimestamp() : Instant.now();

        buffer.addLast(new BufferedEvent(timestamp, payload, event.getUserId()));

        // Evict oldest if over capacity
        while (buffer.size() > MAX_EVENTS) {
            buffer.pollFirst();
        }

        log.debug("[EVENT-BUFFER] Stored event, buffer size = {}", buffer.size());
    }

    /**
     * Returns all events after the given timestamp.
     * Optionally filter by userId or return all (for managers).
     */
    public List<String> getEventsSince(Instant since, String userId, boolean isManager) {
        List<String> missed = new ArrayList<>();
        for (BufferedEvent entry : buffer) {
            if (entry.timestamp().isAfter(since)) {
                // Include if: event belongs to this user, or user is a manager
                if (isManager || entry.userId().equals(userId)) {
                    missed.add(entry.payload());
                }
            }
        }
        log.info("[EVENT-BUFFER] Replaying {} missed events since {}", missed.size(), since);
        return missed;
    }

    public record BufferedEvent(Instant timestamp, String payload, String userId) {}
}
