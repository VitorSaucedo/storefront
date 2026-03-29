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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
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

    private String createToken(String username, String role) {
        var builder = Jwts.builder()
                .subject(username)
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(secretKey);

        if (role != null) {
            builder.claim("role", role);
        }

        return builder.compact();
    }

    @Nested
    @DisplayName("com token válido no header Authorization")
    class ValidToken {

        @Test
        @DisplayName("deve autenticar o usuário com a ROLE_ADMIN correta")
        void shouldAuthenticateAdminUser() throws Exception {
            String token = createToken("admin-user", "ADMIN");
            when(request.getHeader("Authorization")).thenReturn("Bearer " + token);

            jwtAuthFilter.doFilterInternal(request, response, filterChain);

            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            assertThat(auth).isNotNull();
            assertThat(auth.getPrincipal()).isEqualTo("admin-user");

            assertThat(auth.getAuthorities())
                    .extracting(GrantedAuthority::getAuthority)
                    .containsExactly("ROLE_ADMIN");

            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("deve autenticar o usuário com ROLE_USER quando role está ausente no token")
        void shouldAuthenticateWithDefaultRole() throws Exception {
            String token = createToken("standard-user", null);
            when(request.getHeader("Authorization")).thenReturn("Bearer " + token);

            jwtAuthFilter.doFilterInternal(request, response, filterChain);

            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            assertThat(auth.getAuthorities())
                    .extracting(GrantedAuthority::getAuthority)
                    .containsExactly("ROLE_USER");
        }
    }

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
        @DisplayName("deve prosseguir sem autenticar quando token é inválido")
        void shouldPassThroughWhenTokenInvalid() throws Exception {
            when(request.getHeader("Authorization")).thenReturn("Bearer token.invalido.aqui");

            jwtAuthFilter.doFilterInternal(request, response, filterChain);

            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
            verify(filterChain).doFilter(request, response);
        }
    }
}