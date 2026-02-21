package com.steffaneleal.lunnar.dto;

import com.steffaneleal.lunnar.models.AuthProvider;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;

public record RegisterRequestDTO(
        @NotBlank(message = "Nome não pode ser vazio")
        String name,

        @NotBlank(message = "E-mail não pode ser vazio")
        @Email(message = "Formato de e-mail inválido")
        String email,

        @NotBlank(message = "Senha não pode ser vazia")
        String password,

        String companyName,

        AuthProvider provider,
        LocalDate birthdate,
        String phone_number
) {
}
