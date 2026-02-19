package com.steffaneleal.lunnar.dto;

import com.steffaneleal.lunnar.models.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record OrderSummaryDTO(
        UUID orderId,
        LocalDateTime createdAt,
        BigDecimal totalPrice,
        OrderStatus status
) {
}
