package com.steffaneleal.lunnar.dto;

import com.steffaneleal.lunnar.models.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record CustomerReportDTO(
        UUID customerId,
        UUID userId,
        String customerName,
        String customerEmail,
        String companyName,
        int totalOrders,
        BigDecimal totalSpent,
        BigDecimal averageOrderValue,
        LocalDateTime firstPurchaseAt,
        LocalDateTime lastPurchaseAt,
        List<OrderSummaryDTO> ordersSummary,
        List<CategorySummaryDTO> byCategory,
        List<DayOfWeekSummaryDTO> byDayOfWeek
) {
}
