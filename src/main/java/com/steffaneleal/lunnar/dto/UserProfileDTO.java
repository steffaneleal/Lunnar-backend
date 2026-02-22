package com.steffaneleal.lunnar.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record UserProfileDTO(
        UUID id,
        String name,
        String email,
        String role,
        String phoneNumber,
        LocalDate birthdate,
        LocalDateTime createdAt
) {
}