package com.steffaneleal.lunnar.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record ProductRequestDTO(
        String name,
        String description,
        BigDecimal price,
        Integer stockQuantity,
        String imageUrl,
        List<UUID> categoryIds
) {
}