package com.steffaneleal.lunnar.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductDTO(
        UUID id,
        String name,
        String description,
        BigDecimal price,
        Integer stockQuantity,
        UUID categoryId,
        String categoryName,
        String imageUrl
) {
}
