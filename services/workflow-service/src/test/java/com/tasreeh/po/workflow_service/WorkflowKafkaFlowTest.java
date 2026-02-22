package com.tasreeh.po.workflow_service;

import com.tasreeh.po.workflow_service.events.OrderCreatedEvent;
import com.tasreeh.po.workflow_service.events.OrderStatusChangedEvent;
import com.tasreeh.po.workflow_service.service.ApprovalDecisionService;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test verifying the workflow decision logic:
 * - Amount <= 1000 → AUTO_APPROVED status
 * - Amount > 1000 → PENDING_APPROVAL status
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
public class WorkflowKafkaFlowTest {

    @Autowired
    private ApprovalDecisionService decisionService;

    @Test
    @DisplayName("Amount <= 1000 should result in AUTO_APPROVE decision")
    void smallAmount_shouldAutoApprove() {
        var decision = decisionService.decide(new BigDecimal("500"));
        assertEquals(com.tasreeh.po.workflow_service.domain.WorkflowDecision.AUTO_APPROVE, decision);
        log.info("[TEST] ✅ Amount 500 → AUTO_APPROVE");
    }

    @Test
    @DisplayName("Amount > 1000 should require MANAGER_APPROVAL")
    void largeAmount_shouldRequireManagerApproval() {
        var decision = decisionService.decide(new BigDecimal("1500"));
        assertEquals(com.tasreeh.po.workflow_service.domain.WorkflowDecision.PENDING_MANAGER_APPROVAL, decision);
        log.info("[TEST] ✅ Amount 1500 → MANAGER_APPROVAL");
    }

    @Test
    @DisplayName("Amount exactly 1000 should AUTO_APPROVE (boundary test)")
    void boundaryAmount_shouldAutoApprove() {
        var decision = decisionService.decide(new BigDecimal("1000"));
        assertEquals(com.tasreeh.po.workflow_service.domain.WorkflowDecision.AUTO_APPROVE, decision);
        log.info("[TEST] ✅ Amount 1000 → AUTO_APPROVE (boundary)");
    }
}
