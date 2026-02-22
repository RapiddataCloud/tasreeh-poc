package com.tasreeh.po.order_service.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

/**
 * Append-only event log entry for event sourcing.
 * Each state change to a PurchaseOrder is recorded as an immutable event.
 */
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
@Entity
@Table(name = "order_events", indexes = {
        @Index(name = "idx_order_events_order_id", columnList = "orderId")
})
public class OrderEvent {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private UUID orderId;

    @Column(nullable = false)
    private String eventType;

    @Column(columnDefinition = "TEXT")
    private String eventData;

    @Column(nullable = false)
    private String userId;

    @Column
    private String correlationId;

    @Column(nullable = false, updatable = false)
    private Instant timestamp;

    @PrePersist
    void onCreate() {
        if (timestamp == null) timestamp = Instant.now();
    }
}
