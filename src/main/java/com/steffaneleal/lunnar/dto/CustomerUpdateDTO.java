package com.steffaneleal.lunnar.dto;

import java.time.LocalDateTime;

public record CustomerUpdateDTO(
        String companyName,
        String notes,
        LocalDateTime lastContactAt,
        String phoneNumber
) {
}
