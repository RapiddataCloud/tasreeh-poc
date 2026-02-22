package com.tasreeh.po.order_service.events;

import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class OrderStatusChangedEvent {
    private String eventId;
    private String correlationId;
    private UUID orderId;
    private String userId;
    private String previousStatus;
    private String newStatus;
    private String reason;
    private Instant timestamp;
}
