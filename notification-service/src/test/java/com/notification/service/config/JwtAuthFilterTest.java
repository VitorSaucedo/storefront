package com.notification.service.config;

import com.notification.service.util.JwtUtil;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtAuthFilter")
class JwtAuthFilterTest {

    private static final String SECRET =
            "notification-test-secret-key-at-least-32-bytes!!";

    @Mock private HttpServletRequest request;
    @Mock private HttpServletResponse response;
    @Mock private FilterChain filterChain;

    private JwtUtil jwtUtil;
    private JwtAuthFilter jwtAuthFilter;
    private SecretKey secretKey;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil(SECRET);
        jwtAuthFilter = new JwtAuthFilter(jwtUtil);
        secretKey = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        SecurityContextHolder.clearContext();
    }

    private String validToken(String username) {
        return Jwts.builder()
                .subject(username)
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(secretKey)
                .compact();
    }

    // -------------------------------------------------------------------------
    // Token válido
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("com token válido no header Authorization")
    class ValidToken {

        @Test
        @DisplayName("deve autenticar o usuário e chamar filterChain.doFilter")
        void shouldAuthenticateUser() throws Exception {
            String token = validToken("notif-consumer");
            when(request.getHeader("Authorization")).thenReturn("Bearer " + token);

            jwtAuthFilter.doFilterInternal(request, response, filterChain);

            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
            assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal())
                    .isEqualTo("notif-consumer");
            verify(filterChain).doFilter(request, response);
        }
    }

    // -------------------------------------------------------------------------
    // Sem header / header inválido
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("sem header Authorization ou header inválido")
    class NoOrInvalidHeader {

        @Test
        @DisplayName("deve prosseguir sem autenticar quando Authorization está ausente")
        void shouldPassThroughWhenHeaderAbsent() throws Exception {
            when(request.getHeader("Authorization")).thenReturn(null);

            jwtAuthFilter.doFilterInternal(request, response, filterChain);

            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("deve prosseguir sem autenticar quando header não começa com Bearer")
        void shouldPassThroughWhenNotBearer() throws Exception {
            when(request.getHeader("Authorization")).thenReturn("Basic dXNlcjpwYXNz");

            jwtAuthFilter.doFilterInternal(request, response, filterChain);

            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("deve prosseguir sem autenticar quando token é inválido")
        void shouldPassThroughWhenTokenInvalid() throws Exception {
            when(request.getHeader("Authorization")).thenReturn("Bearer token.invalido.aqui");

            jwtAuthFilter.doFilterInternal(request, response, filterChain);

            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("deve prosseguir sem autenticar quando token está expirado")
        void shouldPassThroughWhenTokenExpired() throws Exception {
            String expired = Jwts.builder()
                    .subject("user")
                    .expiration(new Date(System.currentTimeMillis() - 1_000))
                    .signWith(secretKey)
                    .compact();
            when(request.getHeader("Authorization")).thenReturn("Bearer " + expired);

            jwtAuthFilter.doFilterInternal(request, response, filterChain);

            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
            verify(filterChain).doFilter(request, response);
        }
    }
}