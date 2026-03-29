package com.notification.service.util;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("JwtUtil")
class JwtUtilTest {

    private static final String SECRET =
            "notification-test-secret-key-at-least-32-bytes!!";

    private JwtUtil jwtUtil;
    private SecretKey secretKey;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil(SECRET);
        secretKey = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
    }

    private String buildToken(String subject, String role, Date expiration) {
        var builder = Jwts.builder()
                .subject(subject)
                .expiration(expiration)
                .signWith(secretKey);

        if (role != null) {
            builder.claim("role", role);
        }

        return builder.compact();
    }

    private String buildToken(String subject, Date expiration) {
        return buildToken(subject, null, expiration);
    }

    // -------------------------------------------------------------------------
    // extractUsername
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("extractUsername")
    class ExtractUsername {

        @Test
        @DisplayName("deve extrair o username do token válido")
        void shouldExtractUsername() {
            String token = buildToken("notif-user@example.com",
                    new Date(System.currentTimeMillis() + 60_000));

            assertThat(jwtUtil.extractUsername(token)).isEqualTo("notif-user@example.com");
        }
    }

    // -------------------------------------------------------------------------
    // extractRole
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("extractRole")
    class ExtractRole {

        @Test
        @DisplayName("deve extrair a role 'ADMIN' do token corretamente")
        void shouldExtractAdminRole() {
            String token = buildToken("admin@example.com", "ADMIN",
                    new Date(System.currentTimeMillis() + 60_000));

            assertThat(jwtUtil.extractRole(token)).isEqualTo("ADMIN");
        }

        @Test
        @DisplayName("deve retornar null se a claim de role não estiver presente")
        void shouldReturnNullWhenRoleIsMissing() {
            String token = buildToken("user@example.com", null,
                    new Date(System.currentTimeMillis() + 60_000));

            assertThat(jwtUtil.extractRole(token)).isNull();
        }
    }

    // -------------------------------------------------------------------------
    // isTokenValid
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("isTokenValid")
    class IsTokenValid {

        @Test
        @DisplayName("deve retornar true para token bem formado e não expirado")
        void shouldReturnTrueForValidToken() {
            String token = buildToken("user@example.com",
                    new Date(System.currentTimeMillis() + 60_000));

            assertThat(jwtUtil.isTokenValid(token)).isTrue();
        }

        @Test
        @DisplayName("deve retornar false para token expirado")
        void shouldReturnFalseForExpiredToken() {
            String token = buildToken("user@example.com",
                    new Date(System.currentTimeMillis() - 1_000));

            assertThat(jwtUtil.isTokenValid(token)).isFalse();
        }

        @Test
        @DisplayName("deve retornar false para token malformado")
        void shouldReturnFalseForMalformedToken() {
            assertThat(jwtUtil.isTokenValid("not.a.valid.token")).isFalse();
        }

        @Test
        @DisplayName("deve retornar false para string vazia")
        void shouldReturnFalseForEmptyString() {
            assertThat(jwtUtil.isTokenValid("")).isFalse();
        }
    }
}