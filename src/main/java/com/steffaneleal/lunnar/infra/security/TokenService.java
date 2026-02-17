// Geração e validação dos Tokens JWT
package com.steffaneleal.lunnar.infra.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.steffaneleal.lunnar.models.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;


@Service
public class TokenService {
    @Value("${api.security.token.secret}")
    private String secret;

    // Geração do Token
    public String generateToken(User user) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(secret); // algorítmo de hash, dentro do parâmetro vai ter uma chave para o servidor criptografar e descriptografar os dados

            String token = JWT.create()
                           .withIssuer("login-auth-api") // quem está emitindo o token
                           .withSubject(user.getEmail()) // salva o e-mail do usuário no token
                           .withClaim("role", user.getRole().name()) // Adiciona a role do usuário
                           .withExpiresAt(this.generateExpirationDate()) // gera a hora que expira o token
                           .sign(algorithm);             // gera o token

            return token;
        } catch (JWTCreationException exception) {
            throw new RuntimeException("Erro durante a autenticação");
        }
    }

    // Validação do Token
    public String validateToken(String token) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(secret);

            // retorna o token
            return JWT.require(algorithm)
                    .withIssuer("login-auth-api")
                    .build()
                    .verify(token)
                    .getSubject();
        } catch (JWTVerificationException exception) {
            // se não conseguir o token, retorna null
            return null;
        }
    }

    // Geração da data de expiração do token
    private Instant generateExpirationDate() {
        return LocalDateTime.now().plusHours(2).toInstant(ZoneOffset.of("-3")); // validade de 2 horas
    }
}
