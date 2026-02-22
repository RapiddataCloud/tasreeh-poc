package com.tasreeh.po.order_service.api.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class CreateOrderRequest {

    @NotBlank
    private String item;

    @NotNull
    @DecimalMin(value = "0.01")
    private BigDecimal amount;

    private String description;
}

