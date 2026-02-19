package com.steffaneleal.lunnar.dto;

import java.util.UUID;

public record OrderItemRequestDTO(UUID productId, Integer quantity) {
}
