package com.tasreeh.po.order_service.service;

import com.tasreeh.po.order_service.domain.OrderEvent;
import com.tasreeh.po.order_service.domain.OrderStatus;
import com.tasreeh.po.order_service.repository.OrderEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Event Store for event sourcing.
 * Appends immutable events and can replay them to derive current state.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EventStore {

    private final OrderEventRepository eventRepo;

    @Transactional
    public void appendEvent(UUID orderId, String eventType, String eventData, String userId, String correlationId) {
        OrderEvent event = OrderEvent.builder()
                .orderId(orderId)
                .eventType(eventType)
                .eventData(eventData)
                .userId(userId)
                .correlationId(correlationId)
                .timestamp(Instant.now())
                .build();

        eventRepo.save(event);
        log.info("[EVENT-STORE] Appended {} for orderId={}", eventType, orderId);
    }

    /**
     * Replay all events for an order to derive the current status.
     * This demonstrates event sourcing: state derived from event log.
     */
    public OrderStatus replayToCurrentStatus(UUID orderId) {
        List<OrderEvent> events = eventRepo.findByOrderIdOrderByTimestampAsc(orderId);
        if (events.isEmpty()) return null;

        OrderStatus status = OrderStatus.CREATED;
        for (OrderEvent event : events) {
            status = switch (event.getEventType()) {
                case "ORDER_CREATED" -> OrderStatus.CREATED;
                case "STATUS_CHANGED" -> {
                    // Extract newStatus from eventData JSON
                    String newStatus = extractField(event.getEventData(), "newStatus");
                    yield newStatus != null ? OrderStatus.valueOf(newStatus) : status;
                }
                case "ORDER_APPROVED" -> OrderStatus.APPROVED;
                case "ORDER_REJECTED" -> OrderStatus.REJECTED;
                default -> status;
            };
        }

        log.info("[EVENT-STORE] Replayed {} events for orderId={}, derived status={}", events.size(), orderId, status);
        return status;
    }

    public List<OrderEvent> getAllEvents(UUID orderId) {
        return eventRepo.findByOrderIdOrderByTimestampAsc(orderId);
    }

    private String extractField(String json, String key) {
        if (json == null) return null;
        String searchKey = "\"" + key + "\"";
        int keyIdx = json.indexOf(searchKey);
        if (keyIdx < 0) return null;
        int colonIdx = json.indexOf(":", keyIdx + searchKey.length());
        if (colonIdx < 0) return null;
        int startQuote = json.indexOf("\"", colonIdx + 1);
        if (startQuote < 0) return null;
        int endQuote = json.indexOf("\"", startQuote + 1);
        if (endQuote < 0) return null;
        return json.substring(startQuote + 1, endQuote);
    }
}
