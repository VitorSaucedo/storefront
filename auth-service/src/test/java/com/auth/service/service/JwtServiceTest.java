package com.auth.service.service;

import com.auth.service.domain.Role;
import com.auth.service.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private JwtService jwtService;

    private static final String SECRET = "my-super-secret-key-for-testing-purposes-1234";
    private static final long EXPIRATION = 3_600_000L;

    private User mockUser;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secret", SECRET);
        ReflectionTestUtils.setField(jwtService, "expiration", EXPIRATION);

        mockUser = new User();
        mockUser.setId(42L);
        mockUser.setEmail("user@example.com");
        mockUser.setName("John Doe");
        mockUser.setRole(Role.USER);
    }

    // -------------------------------------------------------------------------
    // generateToken()
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("generateToken()")
    class GenerateToken {

        @Test
        @DisplayName("deve gerar um token não nulo e não vazio")
        void shouldReturnNonNullAndNonEmptyToken() {
            String token = jwtService.generateToken(mockUser);

            assertThat(token).isNotNull().isNotBlank();
        }

        @Test
        @DisplayName("deve gerar um token no formato JWT (três segmentos separados por ponto)")
        void shouldReturnTokenWithThreeJwtSegments() {
            String token = jwtService.generateToken(mockUser);

            assertThat(token.split("\\.")).hasSize(3);
        }

        @Test
        @DisplayName("deve gerar tokens diferentes para chamadas distintas (issuedAt diferente)")
        void shouldGenerateDifferentTokensOnSubsequentCalls() throws InterruptedException {
            String token1 = jwtService.generateToken(mockUser);
            Thread.sleep(10); // garante diferença de milissegundos no issuedAt
            String token2 = jwtService.generateToken(mockUser);

            assertThat(token1).isNotNull();
            assertThat(token2).isNotNull();
        }
    }

    // -------------------------------------------------------------------------
    // extractEmail()
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("extractEmail()")
    class ExtractEmail {

        @Test
        @DisplayName("deve extrair o e-mail do subject do token")
        void shouldExtractEmailFromToken() {
            String token = jwtService.generateToken(mockUser);

            String email = jwtService.extractEmail(token);

            assertThat(email).isEqualTo("user@example.com");
        }

        @Test
        @DisplayName("deve extrair corretamente o e-mail para diferentes usuários")
        void shouldExtractCorrectEmailForDifferentUsers() {
            User anotherUser = new User();
            anotherUser.setId(99L);
            anotherUser.setEmail("admin@example.com");
            anotherUser.setName("Admin");
            anotherUser.setRole(Role.ADMIN);

            String token = jwtService.generateToken(anotherUser);

            assertThat(jwtService.extractEmail(token)).isEqualTo("admin@example.com");
        }
    }

    // -------------------------------------------------------------------------
    // extractRole()
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("extractRole()")
    class ExtractRole {

        @Test
        @DisplayName("deve extrair a role USER corretamente do token")
        void shouldExtractUserRoleFromToken() {
            String token = jwtService.generateToken(mockUser);

            String role = jwtService.extractRole(token);

            assertThat(role).isEqualTo("USER");
        }

        @Test
        @DisplayName("deve extrair a role ADMIN corretamente do token")
        void shouldExtractAdminRoleFromToken() {
            mockUser.setRole(Role.ADMIN);
            String token = jwtService.generateToken(mockUser);

            String role = jwtService.extractRole(token);

            assertThat(role).isEqualTo("ADMIN");
        }
    }

    // -------------------------------------------------------------------------
    // isTokenValid()
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("isTokenValid()")
    class IsTokenValid {

        @Test
        @DisplayName("deve retornar true para token recém-gerado e não expirado")
        void shouldReturnTrueForFreshToken() {
            String token = jwtService.generateToken(mockUser);

            assertThat(jwtService.isTokenValid(token)).isTrue();
        }

        @Test
        @DisplayName("deve retornar false para token com assinatura adulterada")
        void shouldReturnFalseForTamperedToken() {
            String token = jwtService.generateToken(mockUser);
            String tampered = token.substring(0, token.length() - 5) + "XXXXX";

            assertThat(jwtService.isTokenValid(tampered)).isFalse();
        }

        @Test
        @DisplayName("deve retornar false para string aleatória (não é um JWT)")
        void shouldReturnFalseForRandomString() {
            assertThat(jwtService.isTokenValid("not.a.valid.jwt")).isFalse();
        }

        @Test
        @DisplayName("deve retornar false para token vazio")
        void shouldReturnFalseForEmptyToken() {
            assertThat(jwtService.isTokenValid("")).isFalse();
        }

        @Test
        @DisplayName("deve retornar false para token expirado")
        void shouldReturnFalseForExpiredToken() {
            ReflectionTestUtils.setField(jwtService, "expiration", -1L);
            String expiredToken = jwtService.generateToken(mockUser);

            assertThat(jwtService.isTokenValid(expiredToken)).isFalse();
        }

        @Test
        @DisplayName("deve retornar false para token assinado com secret diferente")
        void shouldReturnFalseForTokenSignedWithDifferentSecret() {
            JwtService otherService = new JwtService();
            ReflectionTestUtils.setField(otherService, "secret", "completely-different-secret-key-9999");
            ReflectionTestUtils.setField(otherService, "expiration", EXPIRATION);

            String tokenFromOtherService = otherService.generateToken(mockUser);

            assertThat(jwtService.isTokenValid(tokenFromOtherService)).isFalse();
        }
    }

    // -------------------------------------------------------------------------
    // Integração das claims
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("Round-trip: gera e lê as claims do token")
    class RoundTrip {

        @Test
        @DisplayName("email extraído deve coincidir com o e-mail do usuário que gerou o token")
        void emailRoundTrip() {
            String token = jwtService.generateToken(mockUser);
            assertThat(jwtService.extractEmail(token)).isEqualTo(mockUser.getEmail());
        }

        @Test
        @DisplayName("role extraída deve coincidir com a role do usuário que gerou o token")
        void roleRoundTrip() {
            String token = jwtService.generateToken(mockUser);
            assertThat(jwtService.extractRole(token)).isEqualTo(mockUser.getRole().name());
        }

        @Test
        @DisplayName("token gerado deve ser válido imediatamente após a criação")
        void tokenShouldBeValidRightAfterGeneration() {
            String token = jwtService.generateToken(mockUser);
            assertThat(jwtService.isTokenValid(token)).isTrue();
        }
    }
}