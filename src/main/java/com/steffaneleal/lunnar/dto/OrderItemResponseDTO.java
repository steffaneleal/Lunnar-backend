package com.steffaneleal.lunnar.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderItemResponseDTO(UUID productId, String product_name, Integer quantity, BigDecimal unit_price, BigDecimal subtotal) {
}
