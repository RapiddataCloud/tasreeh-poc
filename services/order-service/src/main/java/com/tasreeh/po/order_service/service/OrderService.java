package com.tasreeh.po.order_service.service;

import com.tasreeh.po.order_service.api.dto.CreateOrderRequest;
import com.tasreeh.po.order_service.domain.OrderStatus;
import com.tasreeh.po.order_service.domain.PurchaseOrder;
import com.tasreeh.po.order_service.events.OrderCreatedEvent;
import com.tasreeh.po.order_service.events.OrderStatusChangedEvent;
import com.tasreeh.po.order_service.domain.ProcessedEvent;
import com.tasreeh.po.order_service.repository.ProcessedEventRepository;
import com.tasreeh.po.order_service.repository.PurchaseOrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final PurchaseOrderRepository repo;
    private final ProcessedEventRepository processedEventRepo;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final EventStore eventStore;

    private static final String TOPIC_ORDER_CREATED = "po.order.created";
    private static final String TOPIC_STATUS_CHANGED = "po.order.status-changed";

    @Transactional
    public PurchaseOrder createOrder(String userId, CreateOrderRequest req) {
        PurchaseOrder order = PurchaseOrder.builder()
                .userId(userId)
                .item(req.getItem())
                .amount(req.getAmount())
                .description(req.getDescription())
                .status(OrderStatus.CREATED)
                .build();

        order = repo.save(order);

        log.info("========================================================");
        log.info("[ORDER-SERVICE] Order CREATED in DB");
        log.info("[ORDER-SERVICE]   orderId  = {}", order.getId());
        log.info("[ORDER-SERVICE]   userId   = {}", userId);
        log.info("[ORDER-SERVICE]   item     = {}", order.getItem());
        log.info("[ORDER-SERVICE]   amount   = {}", order.getAmount());
        log.info("[ORDER-SERVICE] >> SENDING to Kafka topic: {}", TOPIC_ORDER_CREATED);
        log.info("========================================================");

        OrderCreatedEvent event = OrderCreatedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .correlationId(UUID.randomUUID().toString())
                .orderId(order.getId())
                .userId(userId)
                .item(order.getItem())
                .amount(order.getAmount())
                .description(order.getDescription())
                .timestamp(Instant.now())
                .build();

        kafkaTemplate.send(TOPIC_ORDER_CREATED, order.getId().toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("========================================================");
                        log.error("[ORDER-SERVICE] FAILED to send to Kafka!");
                        log.error("[ORDER-SERVICE]   error = {}", ex.getMessage());
                        log.error("========================================================");
                    } else {
                        log.info("========================================================");
                        log.info("[ORDER-SERVICE] Kafka CONFIRMED delivery");
                        log.info("[ORDER-SERVICE]   topic     = {}", result.getRecordMetadata().topic());
                        log.info("[ORDER-SERVICE]   partition = {}", result.getRecordMetadata().partition());
                        log.info("[ORDER-SERVICE]   offset    = {}", result.getRecordMetadata().offset());
                        log.info("========================================================");
                    }
                });

        // Event Sourcing: append to event log
        String eventData = String.format(
                "{\"item\":\"%s\",\"amount\":\"%s\",\"description\":\"%s\"}",
                order.getItem(), order.getAmount(), order.getDescription() != null ? order.getDescription() : "");
        eventStore.appendEvent(order.getId(), "ORDER_CREATED", eventData, userId, event.getCorrelationId());

        return order;
    }

    public List<PurchaseOrder> getOrdersForUser(String userId) {
        return repo.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public List<PurchaseOrder> getAllOrders() {
        return repo.findAllByOrderByCreatedAtDesc();
    }

    public List<PurchaseOrder> getOrdersByStatus(OrderStatus status) {
        return repo.findByStatusOrderByCreatedAtDesc(status);
    }

    @Transactional
    public PurchaseOrder approveOrder(UUID orderId, String managerId) {
        PurchaseOrder order = repo.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));

        if (order.getStatus() != OrderStatus.PENDING_APPROVAL) {
            throw new RuntimeException("Order is not in PENDING_APPROVAL status");
        }

        String previousStatus = order.getStatus().name();
        order.setStatus(OrderStatus.APPROVED);
        order = repo.save(order);

        log.info("========================================================");
        log.info("[ORDER-SERVICE] Order APPROVED by manager");
        log.info("[ORDER-SERVICE]   orderId    = {}", orderId);
        log.info("[ORDER-SERVICE]   managerId  = {}", managerId);
        log.info("[ORDER-SERVICE] >> SENDING status-changed to Kafka topic: {}", TOPIC_STATUS_CHANGED);
        log.info("========================================================");

        publishStatusChange(order, previousStatus, "APPROVED",
                "Approved by manager: " + managerId);

        // Event Sourcing: append approval event
        eventStore.appendEvent(orderId, "ORDER_APPROVED",
                String.format("{\"managerId\":\"%s\"}", managerId), managerId, null);

        return order;
    }

    @Transactional
    public PurchaseOrder rejectOrder(UUID orderId, String managerId, String reason) {
        PurchaseOrder order = repo.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));

        if (order.getStatus() != OrderStatus.PENDING_APPROVAL) {
            throw new RuntimeException("Order is not in PENDING_APPROVAL status");
        }

        String previousStatus = order.getStatus().name();
        order.setStatus(OrderStatus.REJECTED);
        order = repo.save(order);

        log.info("========================================================");
        log.info("[ORDER-SERVICE] Order REJECTED by manager");
        log.info("[ORDER-SERVICE]   orderId    = {}", orderId);
        log.info("[ORDER-SERVICE]   managerId  = {}", managerId);
        log.info("[ORDER-SERVICE]   reason     = {}", reason);
        log.info("[ORDER-SERVICE] >> SENDING status-changed to Kafka topic: {}", TOPIC_STATUS_CHANGED);
        log.info("========================================================");

        publishStatusChange(order, previousStatus, "REJECTED",
                reason != null ? reason : "Rejected by manager");

        // Event Sourcing: append rejection event
        eventStore.appendEvent(orderId, "ORDER_REJECTED",
                String.format("{\"managerId\":\"%s\",\"reason\":\"%s\"}", managerId, reason != null ? reason : ""),
                managerId, null);

        return order;
    }

    @Transactional
    public void applyStatusChange(OrderStatusChangedEvent evt) {
        // Idempotency check: skip if already processed
        if (evt.getEventId() != null && processedEventRepo.existsById(evt.getEventId())) {
            log.warn("[ORDER-SERVICE] ⚡ DUPLICATE event skipped: eventId={}", evt.getEventId());
            return;
        }

        PurchaseOrder order = repo.findById(evt.getOrderId()).orElse(null);
        if (order == null) return;

        log.info("========================================================");
        log.info("[ORDER-SERVICE] << RECEIVED status-changed from Kafka");
        log.info("[ORDER-SERVICE]   eventId     = {}", evt.getEventId());
        log.info("[ORDER-SERVICE]   orderId     = {}", evt.getOrderId());
        log.info("[ORDER-SERVICE]   {} -> {}", evt.getPreviousStatus(), evt.getNewStatus());
        log.info("[ORDER-SERVICE]   reason      = {}", evt.getReason());
        log.info("[ORDER-SERVICE] Updating DB...");
        log.info("========================================================");

        order.setStatus(OrderStatus.valueOf(evt.getNewStatus()));
        repo.save(order);

        // Event Sourcing: append status change event
        eventStore.appendEvent(evt.getOrderId(), "STATUS_CHANGED",
                String.format("{\"previousStatus\":\"%s\",\"newStatus\":\"%s\",\"reason\":\"%s\"}",
                        evt.getPreviousStatus(), evt.getNewStatus(), evt.getReason() != null ? evt.getReason() : ""),
                evt.getUserId(), evt.getCorrelationId());

        // Record event as processed
        if (evt.getEventId() != null) {
            processedEventRepo.save(ProcessedEvent.builder()
                    .eventId(evt.getEventId())
                    .eventType("STATUS_CHANGED")
                    .processedAt(Instant.now())
                    .build());
        }
    }

    private void publishStatusChange(PurchaseOrder order, String previousStatus,
                                     String newStatus, String reason) {
        OrderStatusChangedEvent event = OrderStatusChangedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .correlationId(UUID.randomUUID().toString())
                .orderId(order.getId())
                .userId(order.getUserId())
                .previousStatus(previousStatus)
                .newStatus(newStatus)
                .reason(reason)
                .timestamp(Instant.now())
                .build();

        kafkaTemplate.send(TOPIC_STATUS_CHANGED, order.getId().toString(), event);
    }
}
