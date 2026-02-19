package com.steffaneleal.lunnar.infra.security;

import com.steffaneleal.lunnar.models.User;
import com.steffaneleal.lunnar.repositories.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
public class SecurityFilter extends OncePerRequestFilter { // filtro será executado apenas 1 vez a cada requisição que chegar na API

    private static final Logger log = LoggerFactory.getLogger(SecurityFilter.class);

    // Recuperação do Token
    @Autowired
    TokenService tokenService;

    // Verificação do usuário
    @Autowired
    UserRepository userRepository;

    // Metodo do filtro interno
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        var token = this.recoverToken(request);

        if (token == null) {
            log.debug("[JWT] {} {} - Nenhum token no header Authorization", request.getMethod(), request.getRequestURI());
        } else {
            log.debug("[JWT] {} {} - Token presente no header", request.getMethod(), request.getRequestURI());
            var login = tokenService.validateToken(token);

            if (login == null) {
                log.warn("[JWT] {} {} - Token inválido ou expirado", request.getMethod(), request.getRequestURI());
            } else {
                log.debug("[JWT] Token válido para email: {}", login);
                userRepository.findByEmail(login).ifPresentOrElse(
                        user -> {
                            var authorities = Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
                            var authentication = new UsernamePasswordAuthenticationToken(user, null, authorities);
                            SecurityContextHolder.getContext().setAuthentication(authentication);
                            log.info("[JWT] Autenticação definida para {} (role={}) em {}", user.getEmail(), user.getRole(), request.getRequestURI());
                        },
                        () -> log.warn("[JWT] Token válido mas usuário não encontrado no banco: {}", login)
                );
            }
        }

        filterChain.doFilter(request, response);
    }

    // Metodo auxiliar para recuperar e formatar o Token (tirar o Bearer, deixar apenas o Token)
    private String recoverToken(HttpServletRequest request) {
        var authHeader = request.getHeader("Authorization");
        if (authHeader == null) {
            return null;
        }
        // Aceita "Bearer " ou "bearer " (case-insensitive)
        if (!authHeader.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return null;
        }
        var token = authHeader.substring(7).trim();
        return token.isEmpty() ? null : token;
    }
}