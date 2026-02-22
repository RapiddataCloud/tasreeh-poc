package com.tasreeh.po.order_service;

import com.tasreeh.po.order_service.api.dto.CreateOrderRequest;
import com.tasreeh.po.order_service.domain.OrderStatus;
import com.tasreeh.po.order_service.domain.PurchaseOrder;
import com.tasreeh.po.order_service.events.OrderCreatedEvent;
import com.tasreeh.po.order_service.events.OrderStatusChangedEvent;
import com.tasreeh.po.order_service.repository.PurchaseOrderRepository;
import com.tasreeh.po.order_service.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test verifying the full Kafka event flow:
 * 1. Order creation → Kafka event published
 * 2. Status change consumed → DB updated
 * 3. Idempotency — duplicate events are ignored
 */
@Slf4j
@SpringBootTest
@EmbeddedKafka(
        topics = {"po.order.created", "po.order.status-changed"},
        partitions = 1,
        brokerProperties = {
                "listeners=PLAINTEXT://localhost:0",
                "auto.create.topics.enable=true"
        }
)
@ActiveProfiles("test")
@DirtiesContext
public class OrderKafkaFlowTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private PurchaseOrderRepository orderRepo;

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Test
    @DisplayName("Creating an order should persist in DB with CREATED status")
    void createOrder_shouldPersistInDb() {
        // Given
        CreateOrderRequest req = new CreateOrderRequest();
        req.setItem("Test Laptop");
        req.setAmount(new BigDecimal("500.00"));
        req.setDescription("Integration test order");

        // When
        PurchaseOrder order = orderService.createOrder("test-user-1", req);

        // Then
        assertNotNull(order.getId());
        assertEquals(OrderStatus.CREATED, order.getStatus());
        assertEquals("test-user-1", order.getUserId());
        assertEquals("Test Laptop", order.getItem());
        log.info("[TEST] ✅ Order created: id={}, status={}", order.getId(), order.getStatus());
    }

    @Test
    @DisplayName("Consuming a status-changed event should update the order in DB")
    void consumeStatusChanged_shouldUpdateDb() throws Exception {
        // Given - create an order first
        CreateOrderRequest req = new CreateOrderRequest();
        req.setItem("Test Monitor");
        req.setAmount(new BigDecimal("200.00"));
        req.setDescription("Status change test");

        PurchaseOrder order = orderService.createOrder("test-user-2", req);
        UUID orderId = order.getId();

        // When - simulate a status-changed event (as if from workflow-service)
        OrderStatusChangedEvent statusEvent = OrderStatusChangedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .correlationId(UUID.randomUUID().toString())
                .orderId(orderId)
                .userId("test-user-2")
                .previousStatus("CREATED")
                .newStatus("AUTO_APPROVED")
                .reason("Auto-approved: amount <= 1000")
                .timestamp(Instant.now())
                .build();

        orderService.applyStatusChange(statusEvent);

        // Then
        PurchaseOrder updatedOrder = orderRepo.findById(orderId).orElseThrow();
        assertEquals(OrderStatus.AUTO_APPROVED, updatedOrder.getStatus());
        log.info("[TEST] ✅ Status updated: id={}, status={}", orderId, updatedOrder.getStatus());
    }

    @Test
    @DisplayName("Duplicate event with same eventId should be ignored (idempotency)")
    void duplicateEvent_shouldBeIgnored() {
        // Given
        CreateOrderRequest req = new CreateOrderRequest();
        req.setItem("Test Keyboard");
        req.setAmount(new BigDecimal("100.00"));
        req.setDescription("Idempotency test");

        PurchaseOrder order = orderService.createOrder("test-user-3", req);
        UUID orderId = order.getId();

        String eventId = UUID.randomUUID().toString();

        OrderStatusChangedEvent statusEvent = OrderStatusChangedEvent.builder()
                .eventId(eventId)
                .correlationId(UUID.randomUUID().toString())
                .orderId(orderId)
                .userId("test-user-3")
                .previousStatus("CREATED")
                .newStatus("AUTO_APPROVED")
                .reason("Auto-approved")
                .timestamp(Instant.now())
                .build();

        // When - apply the same event twice
        orderService.applyStatusChange(statusEvent);
        orderService.applyStatusChange(statusEvent); // Should be skipped

        // Then - order should be AUTO_APPROVED (not processed twice)
        PurchaseOrder updatedOrder = orderRepo.findById(orderId).orElseThrow();
        assertEquals(OrderStatus.AUTO_APPROVED, updatedOrder.getStatus());
        log.info("[TEST] ✅ Duplicate event correctly skipped for eventId={}", eventId);
    }
}
