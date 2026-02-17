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
            if (secret == null || secret.isEmpty()) {
                throw new RuntimeException("JWT_SECRET não está configurado. Verifique as variáveis de ambiente.");
            }

            Algorithm algorithm = Algorithm.HMAC256(secret); // algorítmo de hash, dentro do parâmetro vai ter uma chave para o servidor criptografar e descriptografar os dados

            String token = JWT.create()
                           .withIssuer("login-auth-api") // quem está emitindo o token
                           .withSubject(user.getEmail()) // salva o e-mail do usuário no token
                           .withClaim("role", user.getRole().name()) // Adiciona a role do usuário
                           .withExpiresAt(this.generateExpirationDate()) // gera a hora que expira o token
                           .sign(algorithm);             // gera o token

            return token;
        } catch (JWTCreationException exception) {
            throw new RuntimeException("Erro durante a geração do token: " + exception.getMessage(), exception);
        } catch (IllegalArgumentException exception) {
            throw new RuntimeException("Erro na configuração do algoritmo JWT: " + exception.getMessage(), exception);
        }
    }

    // Validação do Token
    public String validateToken(String token) {
        try {
            if (token == null || token.isEmpty()) {
                return null;
            }

            if (secret == null || secret.isEmpty()) {
                throw new RuntimeException("JWT_SECRET não está configurado. Verifique as variáveis de ambiente.");
            }

            Algorithm algorithm = Algorithm.HMAC256(secret);

            // retorna o email do usuário (subject do token)
            return JWT.require(algorithm)
                    .withIssuer("login-auth-api")
                    .build()
                    .verify(token)
                    .getSubject();
        } catch (JWTVerificationException exception) {
            // se não conseguir validar o token, retorna null
            return null;
        } catch (IllegalArgumentException exception) {
            // erro na configuração do algoritmo
            return null;
        }
    }

    // Geração da data de expiração do token
    private Instant generateExpirationDate() {
        return LocalDateTime.now().plusHours(2).toInstant(ZoneOffset.of("-03:00")); // validade de 2 horas
    }
}