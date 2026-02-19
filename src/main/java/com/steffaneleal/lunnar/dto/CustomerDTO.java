package com.steffaneleal.lunnar.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record CustomerDTO(
        UUID id,
        UUID userId,
        String userName,
        String userEmail,
        String phoneNumber,
        String companyName,
        String notes,
        LocalDateTime lastContactAt,
        LocalDateTime createdAt
) {
}
