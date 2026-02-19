package com.steffaneleal.lunnar.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderItemResponseDTO(UUID productId, String productName, Integer quantity, BigDecimal unitPrice, BigDecimal subtotal) {
}
