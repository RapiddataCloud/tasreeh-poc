package com.tasreeh.po.order_service.kafka;

import com.tasreeh.po.order_service.events.OrderStatusChangedEvent;
import com.tasreeh.po.order_service.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.DltStrategy;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderStatusChangedConsumer {

    private final OrderService orderService;

    @RetryableTopic(
            attempts = "3",
            backoff = @Backoff(delay = 1000, multiplier = 2),
            dltStrategy = DltStrategy.ALWAYS_RETRY_ON_ERROR,
            autoCreateTopics = "true"
    )
    @KafkaListener(
            topics = "po.order.status-changed",
            containerFactory = "statusChangedKafkaListenerFactory"
    )
    public void consume(OrderStatusChangedEvent event) {
        log.info("[ORDER-SERVICE] Processing status-changed event for orderId={}", event.getOrderId());
        orderService.applyStatusChange(event);
    }

    @DltHandler
    public void handleDlt(OrderStatusChangedEvent event) {
        log.error("========================================================");
        log.error("[ORDER-SERVICE]   DLT — Failed after all retries");
        log.error("[ORDER-SERVICE]   orderId = {}", event.getOrderId());
        log.error("[ORDER-SERVICE]   status  = {} -> {}", event.getPreviousStatus(), event.getNewStatus());
        log.error("[ORDER-SERVICE]   reason  = {}", event.getReason());
        log.error("[ORDER-SERVICE]   Event sent to: po.order.status-changed-dlt");
        log.error("========================================================");
    }
}

