package com.tasreeh.po.order_service.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
@Entity
@Table(name = "processed_events")
public class ProcessedEvent {

    @Id
    private String eventId;

    @Column(nullable = false)
    private String eventType;

    @Column(nullable = false)
    private Instant processedAt;

    @PrePersist
    void onCreate() {
        if (processedAt == null) processedAt = Instant.now();
    }
}
