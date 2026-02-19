// Controller com os endpoints de login e cadastro de usuários
package com.steffaneleal.lunnar.controllers;

import com.steffaneleal.lunnar.dto.LoginRequestDTO;
import com.steffaneleal.lunnar.dto.RegisterRequestDTO;
import com.steffaneleal.lunnar.dto.ResponseDTO;
import com.steffaneleal.lunnar.infra.security.TokenService;
import com.steffaneleal.lunnar.models.Customer;
import com.steffaneleal.lunnar.models.User;
import com.steffaneleal.lunnar.models.UserRole;
import com.steffaneleal.lunnar.repositories.CustomerRepository;
import com.steffaneleal.lunnar.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final UserRepository repository;
    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    // Login
    @PostMapping("/login") // /auth/login
    public ResponseEntity login(@RequestBody LoginRequestDTO body ) {
        User user = this.repository.findByEmail(body.email()).orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        // Verificação da senha
        if (passwordEncoder.matches(body.password(), user.getPassword())) {
            String token = this.tokenService.generateToken(user);

            return  ResponseEntity.ok(new ResponseDTO(user.getName(), token)); // verificar no frontend quais são as respostas esperadas
        }

        return ResponseEntity.badRequest().build();
    }

    // Registro
    @PostMapping("/register")
    public ResponseEntity register(@RequestBody RegisterRequestDTO body) {
        Optional<User> user = this.repository.findByEmail(body.email());

        // Criando um novo usuário
        if(user.isEmpty()) {
            User newUser = new User();

            // Salvando a senha de forma criptografada no banco de dados
            newUser.setPassword(passwordEncoder.encode(body.password()));

            newUser.setEmail(body.email());
            newUser.setName(body.name());
            newUser.setProvider(body.provider());
            newUser.setBirthdate(body.birthdate());
            newUser.setPhoneNumber(body.phone_number());
            newUser.setRole(UserRole.USER);
            this.repository.save(newUser);

            // Cria registro de cliente (CRM) para usuários com role USER
            Customer customer = new Customer();
            customer.setUser(newUser);
            customerRepository.save(customer);

            // Geração do token
            String token = this.tokenService.generateToken(newUser);
            return ResponseEntity.ok(new ResponseDTO(newUser.getName(), token));
        }

        return ResponseEntity.badRequest().build();
    }

    // Rota para testar o token: retorna o usuário autenticado (requer Authorization: Bearer <token>)
    @GetMapping("/me")
    public ResponseEntity<?> me(@AuthenticationPrincipal User user) {
        if (user == null) {
            log.warn("[Auth] GET /auth/me - Requisição sem usuário autenticado (401)");
            return ResponseEntity.status(401).build();
        }
        log.info("[Auth] GET /auth/me - Usuário autenticado: {} ({})", user.getEmail(), user.getRole());
        return ResponseEntity.ok(Map.of(
                "id", user.getId(),
                "name", user.getName(),
                "email", user.getEmail(),
                "role", user.getRole().name()
        ));
    }

    // Rota para testar se o token funciona e a autorização.
        // Requer: Authorization: Bearer &lt;token&gt;
        // Retorna mensagem de sucesso com dados do usuário e confirmação de token + autorização
    @GetMapping("/test")
    public ResponseEntity<?> testTokenAndAuth(@AuthenticationPrincipal User user) {
        if (user == null) {
            log.warn("[Auth] GET /auth/test - Token ausente ou inválido (401)");
            return ResponseEntity.status(401).body(Map.of(
                    "success", false,
                    "message", "Token ausente ou inválido. Envie: Authorization: Bearer <seu_token>"
            ));
        }
        log.info("[Auth] GET /auth/test - Token OK | usuário={} | role={}", user.getEmail(), user.getRole());
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Token válido e autorização OK.",
                "user", Map.of(
                        "id", user.getId().toString(),
                        "name", user.getName(),
                        "email", user.getEmail(),
                        "role", user.getRole().name()
                )
        ));
    }

    // Rota para testar autorização de ADMIN -> Só retorna 200 se o token for de um usuário com role ADMIN; caso contrário 403.
    @GetMapping("/test-admin")
    public ResponseEntity<?> testAdminOnly(@AuthenticationPrincipal User user) {
        if (user == null) {
            log.warn("[Auth] GET /auth/test-admin - Não autenticado (401)");
            return ResponseEntity.status(401).body(Map.of(
                    "success", false,
                    "message", "Token ausente ou inválido."
            ));
        }
        if (user.getRole() != UserRole.ADMIN) {
            log.warn("[Auth] GET /auth/test-admin - Acesso negado para {} (role={}, esperado ADMIN)", user.getEmail(), user.getRole());
            return ResponseEntity.status(403).body(Map.of(
                    "success", false,
                    "message", "Acesso negado. Apenas ADMIN pode acessar esta rota.",
                    "yourRole", user.getRole().name()
            ));
        }
        log.info("[Auth] GET /auth/test-admin - Acesso ADMIN autorizado: {}", user.getEmail());
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Você é ADMIN. Autorização OK.",
                "user", user.getEmail()
        ));
    }
}
