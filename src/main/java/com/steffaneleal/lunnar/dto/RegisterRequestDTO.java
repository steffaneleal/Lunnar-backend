package com.steffaneleal.lunnar.dto;

import com.steffaneleal.lunnar.models.AuthProvider;
import java.time.LocalDate;

public record RegisterRequestDTO (String name, String email, String password, AuthProvider provider, LocalDate birthdate, String phone_number) {
}
