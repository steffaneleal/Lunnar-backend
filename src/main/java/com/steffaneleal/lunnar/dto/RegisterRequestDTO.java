package com.steffaneleal.lunnar.dto;

import java.time.LocalDate;

public record RegisterRequestDTO (String name, String email, String password, String provider, LocalDate birthdate, String phone_number) {
}
