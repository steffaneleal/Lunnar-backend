package com.steffaneleal.lunnar.controllers;

import com.steffaneleal.lunnar.dto.UserProfileDTO;
import com.steffaneleal.lunnar.dto.UserUpdateDTO;
import com.steffaneleal.lunnar.models.User;
import com.steffaneleal.lunnar.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;

    // Atualiza nome e/ou e-mail do próprio usuário logado
    @PutMapping("/me")
    @Transactional
    public ResponseEntity<?> updateMe(
            @RequestBody UserUpdateDTO dto,
            @AuthenticationPrincipal User authenticatedUser) {

        User user = userRepository.findById(authenticatedUser.getId())
                .orElseThrow(() -> new IllegalStateException("Usuário não encontrado."));

        // Valida e atualiza e-mail (apenas se mudou)
        if (dto.email() != null && !dto.email().isBlank() && !dto.email().equals(user.getEmail())) {
            if (userRepository.findByEmail(dto.email()).isPresent()) {
                return ResponseEntity.badRequest().body("E-mail já está em uso.");
            }
            user.setEmail(dto.email());
        }

        if (dto.name() != null && !dto.name().isBlank()) {
            user.setName(dto.name());
        }

        userRepository.save(user);

        return ResponseEntity.ok(new UserProfileDTO(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole().name()
        ));
    }
}