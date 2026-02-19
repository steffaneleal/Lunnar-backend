package com.steffaneleal.lunnar.dto;

import java.math.BigDecimal;

public record CategorySummaryDTO(
        String categoryName,
        int orderCount,
        int totalQuantity,
        BigDecimal totalValue
) {
}
