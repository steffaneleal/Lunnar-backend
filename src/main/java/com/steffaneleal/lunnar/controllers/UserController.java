package com.steffaneleal.lunnar.controllers;

import com.steffaneleal.lunnar.dto.UserProfileDTO;
import com.steffaneleal.lunnar.dto.UserUpdateDTO;
import com.steffaneleal.lunnar.models.User;
import com.steffaneleal.lunnar.models.UserRole;
import com.steffaneleal.lunnar.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;

    // Lista todos os usuários com detalhes completos — apenas ADMIN
    @GetMapping
    public ResponseEntity<List<UserProfileDTO>> listAll(@AuthenticationPrincipal User admin) {
        if (admin.getRole() != UserRole.ADMIN) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        List<UserProfileDTO> users = userRepository.findAll().stream()
                .sorted(Comparator.comparing(User::getName, String.CASE_INSENSITIVE_ORDER))
                .map(this::toDTO)
                .toList();
        return ResponseEntity.ok(users);
    }

    // Atualiza nome e/ou e-mail do próprio usuário logado
    @PutMapping("/me")
    @Transactional
    public ResponseEntity<?> updateMe(
            @RequestBody UserUpdateDTO dto,
            @AuthenticationPrincipal User authenticatedUser) {

        User user = userRepository.findById(authenticatedUser.getId())
                .orElseThrow(() -> new IllegalStateException("Usuário não encontrado."));

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
        return ResponseEntity.ok(toDTO(user));
    }

    // Promove ou rebaixa o role de um usuário — apenas ADMIN
    @PatchMapping("/{id}/role")
    @Transactional
    public ResponseEntity<?> updateRole(
            @PathVariable UUID id,
            @RequestBody RoleUpdateRequest body,
            @AuthenticationPrincipal User admin) {

        if (admin.getRole() != UserRole.ADMIN) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Apenas administradores podem alterar roles.");
        }

        if (admin.getId().equals(id)) {
            return ResponseEntity.badRequest().body("Você não pode alterar o seu próprio role.");
        }

        UserRole novoRole;
        try {
            novoRole = UserRole.valueOf(body.role().toUpperCase());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Role inválido. Use ADMIN ou USER.");
        }

        return userRepository.findById(id)
                .map(target -> {
                    target.setRole(novoRole);
                    userRepository.save(target);
                    return ResponseEntity.ok((Object) toDTO(target));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    private UserProfileDTO toDTO(User u) {
        return new UserProfileDTO(
                u.getId(),
                u.getName(),
                u.getEmail(),
                u.getRole().name(),
                u.getPhoneNumber(),
                u.getBirthdate(),
                u.getCreatedAt()
        );
    }

    record RoleUpdateRequest(String role) {}
}