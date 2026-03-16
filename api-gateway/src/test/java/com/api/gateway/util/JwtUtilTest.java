package com.api.gateway.util;

import io.jsonwebtoken.Claims;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("JwtUtil")
class JwtUtilTest {

    private static final String SECRET = "minha-chave-secreta-com-pelo-menos-32-bytes!!";

    private JwtUtil jwtUtil;
    private SecretKey secretKey;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil(SECRET);
        secretKey = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
    }

    private String buildToken(String subject, String role, long expiresInMs) {
        return Jwts.builder()
                .subject(subject)
                .claim("role", role)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiresInMs))
                .signWith(secretKey)
                .compact();
    }

    // -------------------------------------------------------------------------
    // extractClaims
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("extractClaims")
    class ExtractClaims {

        @Test
        @DisplayName("deve extrair claims de token válido")
        void shouldExtractClaimsFromValidToken() {
            String token = buildToken("user@example.com", "USER", 60_000);

            Claims claims = jwtUtil.extractClaims(token);

            assertThat(claims.getSubject()).isEqualTo("user@example.com");
            assertThat(claims.get("role", String.class)).isEqualTo("USER");
        }

        @Test
        @DisplayName("deve lançar exceção para token expirado")
        void shouldThrowForExpiredToken() {
            String token = buildToken("user@example.com", "USER", -1_000);

            assertThatThrownBy(() -> jwtUtil.extractClaims(token))
                    .isInstanceOf(Exception.class);
        }

        @Test
        @DisplayName("deve lançar exceção para token com assinatura inválida")
        void shouldThrowForInvalidSignature() {
            SecretKey wrongKey = Keys.hmacShaKeyFor(
                    "outra-chave-secreta-completamente-diferente!!".getBytes(StandardCharsets.UTF_8)
            );
            String token = Jwts.builder()
                    .subject("user@example.com")
                    .signWith(wrongKey)
                    .compact();

            assertThatThrownBy(() -> jwtUtil.extractClaims(token))
                    .isInstanceOf(Exception.class);
        }

        @Test
        @DisplayName("deve lançar exceção para token malformado")
        void shouldThrowForMalformedToken() {
            assertThatThrownBy(() -> jwtUtil.extractClaims("nao.e.um.jwt.valido"))
                    .isInstanceOf(Exception.class);
        }
    }

    // -------------------------------------------------------------------------
    // isValid
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("isValid")
    class IsValid {

        @Test
        @DisplayName("deve retornar true para token válido")
        void shouldReturnTrueForValidToken() {
            String token = buildToken("user@example.com", "USER", 60_000);

            assertThat(jwtUtil.isValid(token)).isTrue();
        }

        @Test
        @DisplayName("deve retornar false para token expirado")
        void shouldReturnFalseForExpiredToken() {
            String token = buildToken("user@example.com", "USER", -1_000);

            assertThat(jwtUtil.isValid(token)).isFalse();
        }

        @Test
        @DisplayName("deve retornar false para token com assinatura inválida")
        void shouldReturnFalseForInvalidSignature() {
            SecretKey wrongKey = Keys.hmacShaKeyFor(
                    "outra-chave-secreta-completamente-diferente!!".getBytes(StandardCharsets.UTF_8)
            );
            String token = Jwts.builder()
                    .subject("user@example.com")
                    .signWith(wrongKey)
                    .compact();

            assertThat(jwtUtil.isValid(token)).isFalse();
        }

        @Test
        @DisplayName("deve retornar false para token malformado")
        void shouldReturnFalseForMalformedToken() {
            assertThat(jwtUtil.isValid("isto.nao.e.jwt")).isFalse();
        }

        @Test
        @DisplayName("deve retornar false para string vazia")
        void shouldReturnFalseForEmptyString() {
            assertThat(jwtUtil.isValid("")).isFalse();
        }
    }

    // -------------------------------------------------------------------------
    // extractUsername
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("extractUsername")
    class ExtractUsername {

        @Test
        @DisplayName("deve retornar o subject do token")
        void shouldReturnSubject() {
            String token = buildToken("admin@example.com", "ADMIN", 60_000);
            Claims claims = jwtUtil.extractClaims(token);

            assertThat(jwtUtil.extractUsername(claims)).isEqualTo("admin@example.com");
        }
    }

    // -------------------------------------------------------------------------
    // extractRole
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("extractRole")
    class ExtractRole {

        @Test
        @DisplayName("deve retornar a role do token")
        void shouldReturnRole() {
            String token = buildToken("user@example.com", "ADMIN", 60_000);
            Claims claims = jwtUtil.extractClaims(token);

            assertThat(jwtUtil.extractRole(claims)).isEqualTo("ADMIN");
        }

        @Test
        @DisplayName("deve retornar 'UNKNOWN' quando claim role está ausente")
        void shouldReturnUnknownWhenRoleIsMissing() {
            String tokenSemRole = Jwts.builder()
                    .subject("user@example.com")
                    .issuedAt(new Date())
                    .expiration(new Date(System.currentTimeMillis() + 60_000))
                    .signWith(secretKey)
                    .compact();
            Claims claims = jwtUtil.extractClaims(tokenSemRole);

            assertThat(jwtUtil.extractRole(claims)).isEqualTo("UNKNOWN");
        }
    }
}