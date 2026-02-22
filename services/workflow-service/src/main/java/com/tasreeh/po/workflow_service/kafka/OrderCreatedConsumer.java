package com.tasreeh.po.workflow_service.kafka;
import com.tasreeh.po.workflow_service.domain.WorkflowDecision;
import com.tasreeh.po.workflow_service.events.OrderCreatedEvent;
import com.tasreeh.po.workflow_service.events.OrderStatusChangedEvent;
import com.tasreeh.po.workflow_service.service.ApprovalDecisionService;
import com.tasreeh.po.workflow_service.service.StatusPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.DltStrategy;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static com.tasreeh.po.workflow_service.bpmn.BpmnElements.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderCreatedConsumer {

    private final ApprovalDecisionService decisionService;
    private final StatusPublisher publisher;

    // In-memory idempotency tracker (stateless service — no DB)
    private final Set<String> processedEventIds = ConcurrentHashMap.newKeySet();

    @RetryableTopic(
            attempts = "3",
            backoff = @Backoff(delay = 1000, multiplier = 2),
            dltStrategy = DltStrategy.ALWAYS_RETRY_ON_ERROR,
            autoCreateTopics = "true"
    )
    @KafkaListener(topics = "po.order.created")
    public void consume(OrderCreatedEvent event) {

        // Idempotency check
        if (event.getEventId() != null && !processedEventIds.add(event.getEventId())) {
            log.warn("[WORKFLOW-SERVICE] ⚡ DUPLICATE event skipped: eventId={}", event.getEventId());
            return;
        }

        log.info("========================================================");
        log.info("[WORKFLOW-SERVICE] << RECEIVED from Kafka topic: po.order.created");
        log.info("[WORKFLOW-SERVICE]   eventId  = {}", event.getEventId());
        log.info("[WORKFLOW-SERVICE]   orderId  = {}", event.getOrderId());
        log.info("[WORKFLOW-SERVICE]   userId   = {}", event.getUserId());
        log.info("[WORKFLOW-SERVICE]   item     = {}", event.getItem());
        log.info("[WORKFLOW-SERVICE]   amount   = {}", event.getAmount());
        log.info("========================================================");

        // BPMN: Exclusive Gateway (Amount > 1000?)
        WorkflowDecision decision = decisionService.decide(event.getAmount());

        String newStatus;
        String reason;

        if (decision == WorkflowDecision.AUTO_APPROVE) {
            log.info("[WORKFLOW-SERVICE] BPMN Decision: AUTO_APPROVE (amount <= 1000)");
            newStatus = "AUTO_APPROVED";
            reason = "Auto-approved: amount <= 1000";
        } else {
            log.info("[WORKFLOW-SERVICE] BPMN Decision: MANAGER_APPROVAL (amount > 1000)");
            newStatus = "PENDING_APPROVAL";
            reason = "Manager approval required: amount > 1000";
        }

        OrderStatusChangedEvent statusChanged = OrderStatusChangedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .correlationId(event.getCorrelationId())
                .orderId(event.getOrderId())
                .userId(event.getUserId())
                .previousStatus("CREATED")
                .newStatus(newStatus)
                .reason(reason)
                .timestamp(Instant.now())
                .build();

        log.info("========================================================");
        log.info("[WORKFLOW-SERVICE] >> SENDING to Kafka topic: po.order.status-changed");
        log.info("[WORKFLOW-SERVICE]   orderId    = {}", event.getOrderId());
        log.info("[WORKFLOW-SERVICE]   newStatus  = {}", newStatus);
        log.info("[WORKFLOW-SERVICE]   reason     = {}", reason);
        log.info("========================================================");

        publisher.publish(statusChanged);
    }

    @DltHandler
    public void handleDlt(OrderCreatedEvent event) {
        log.error("========================================================");
        log.error("[WORKFLOW-SERVICE] ❌ DLT — Failed after all retries");
        log.error("[WORKFLOW-SERVICE]   orderId = {}", event.getOrderId());
        log.error("[WORKFLOW-SERVICE]   userId  = {}", event.getUserId());
        log.error("[WORKFLOW-SERVICE]   amount  = {}", event.getAmount());
        log.error("[WORKFLOW-SERVICE]   Event sent to: po.order.created-dlt");
        log.error("========================================================");
    }
}
