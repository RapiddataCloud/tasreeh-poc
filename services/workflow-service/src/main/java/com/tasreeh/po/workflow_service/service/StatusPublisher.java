package com.tasreeh.po.workflow_service.service;

import com.tasreeh.po.workflow_service.events.OrderStatusChangedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StatusPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private static final String TOPIC_STATUS_CHANGED = "po.order.status-changed";

    public void publish(OrderStatusChangedEvent event) {
        kafkaTemplate.send(TOPIC_STATUS_CHANGED, event.getOrderId().toString(), event);
    }
}

