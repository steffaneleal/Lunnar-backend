// Controller com os endpoints de login e cadastro de usuários
package com.steffaneleal.lunnar.controllers;

import com.steffaneleal.lunnar.dto.LoginRequestDTO;
import com.steffaneleal.lunnar.dto.RegisterRequestDTO;
import com.steffaneleal.lunnar.dto.ResponseDTO;
import com.steffaneleal.lunnar.infra.security.TokenService;
import com.steffaneleal.lunnar.models.User;
import com.steffaneleal.lunnar.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor // gerar o construtor da classe com esses atributos como parâmetro (mesma coisa de colocar um @Autowired em cima de cada um)
public class AuthController {
    private final UserRepository repository;
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
            newUser.setRole(com.steffaneleal.lunnar.models.UserRole.ADMIN);
            
            this.repository.save(newUser);

            // Geração do token
            String token = this.tokenService.generateToken(newUser);
            return ResponseEntity.ok(new ResponseDTO(newUser.getName(), token));
        }

        return ResponseEntity.badRequest().build();
    }
}
