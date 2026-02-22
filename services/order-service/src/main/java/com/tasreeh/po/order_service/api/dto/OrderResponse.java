package com.tasreeh.po.order_service.api.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class OrderResponse {
    private UUID id;
    private String item;
    private BigDecimal amount;
    private String description;
    private String status;
    private String submittedBy;
    private Instant createdAt;
}
