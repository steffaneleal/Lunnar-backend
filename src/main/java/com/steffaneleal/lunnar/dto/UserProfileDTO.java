package com.steffaneleal.lunnar.dto;

import java.util.UUID;

public record UserProfileDTO(
        UUID id,
        String name,
        String email,
        String role
) {
}
