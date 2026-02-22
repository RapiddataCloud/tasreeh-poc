package com.tasreeh.po.workflow_service.events;

import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class OrderCreatedEvent {
    private String eventId;
    private String correlationId;
    private UUID orderId;
    private String userId;
    private String item;
    private BigDecimal amount;
    private String description;
    private Instant timestamp;
}

