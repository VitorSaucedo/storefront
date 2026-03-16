package com.api.gateway.config;

import com.api.gateway.util.JwtUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class JwtAuthenticationFilterTest {

    private static final String SECRET = "minha-chave-secreta-com-pelo-menos-32-bytes!!";

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private GatewayFilterChain chain;

    @InjectMocks
    private JwtAuthenticationFilter filter;

    private String buildToken(String subject, String role) {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .subject(subject)
                .claim("role", role)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(key)
                .compact();
    }

    // -------------------------------------------------------------------------
    // Paths públicos
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("paths públicos")
    class PublicPaths {

        @Test
        @DisplayName("deve passar /auth/register sem verificar token")
        void shouldPassAuthRegisterWithoutToken() {
            when(chain.filter(any())).thenReturn(Mono.empty());

            MockServerWebExchange exchange = MockServerWebExchange.from(
                    MockServerHttpRequest.get("/auth/register").build()
            );

            StepVerifier.create(filter.filter(exchange, chain))
                    .verifyComplete();

            verify(chain).filter(any());
            verify(jwtUtil, never()).extractClaims(any());
        }

        @Test
        @DisplayName("deve passar /auth/login sem verificar token")
        void shouldPassAuthLoginWithoutToken() {
            when(chain.filter(any())).thenReturn(Mono.empty());

            MockServerWebExchange exchange = MockServerWebExchange.from(
                    MockServerHttpRequest.get("/auth/login").build()
            );

            StepVerifier.create(filter.filter(exchange, chain))
                    .verifyComplete();

            verify(chain).filter(any());
            verify(jwtUtil, never()).extractClaims(any());
        }

        @Test
        @DisplayName("deve passar subpaths de /auth/register (ex: /auth/register/confirm)")
        void shouldPassSubpathsOfPublicPaths() {
            when(chain.filter(any())).thenReturn(Mono.empty());

            MockServerWebExchange exchange = MockServerWebExchange.from(
                    MockServerHttpRequest.get("/auth/register/confirm").build()
            );

            StepVerifier.create(filter.filter(exchange, chain))
                    .verifyComplete();

            verify(chain).filter(any());
        }
    }

    // -------------------------------------------------------------------------
    // Requisições sem token
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("header Authorization ausente ou inválido")
    class MissingOrInvalidHeader {

        @Test
        @DisplayName("deve retornar 401 quando header Authorization está ausente")
        void shouldReturn401WhenAuthHeaderMissing() {
            MockServerWebExchange exchange = MockServerWebExchange.from(
                    MockServerHttpRequest.get("/orders").build()
            );

            StepVerifier.create(filter.filter(exchange, chain))
                    .verifyComplete();

            assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
            verify(chain, never()).filter(any());
        }

        @Test
        @DisplayName("deve retornar 401 quando header não começa com 'Bearer '")
        void shouldReturn401WhenHeaderDoesNotStartWithBearer() {
            MockServerWebExchange exchange = MockServerWebExchange.from(
                    MockServerHttpRequest.get("/orders")
                            .header(HttpHeaders.AUTHORIZATION, "Basic dXNlcjpwYXNz")
                            .build()
            );

            StepVerifier.create(filter.filter(exchange, chain))
                    .verifyComplete();

            assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
            verify(chain, never()).filter(any());
        }
    }

    // -------------------------------------------------------------------------
    // Token inválido
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("token inválido ou expirado")
    class InvalidToken {

        @Test
        @DisplayName("deve retornar 401 quando token é inválido")
        void shouldReturn401WhenTokenIsInvalid() {
            when(jwtUtil.extractClaims("token-invalido"))
                    .thenThrow(new RuntimeException("JWT inválido"));

            MockServerWebExchange exchange = MockServerWebExchange.from(
                    MockServerHttpRequest.get("/orders")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer token-invalido")
                            .build()
            );

            StepVerifier.create(filter.filter(exchange, chain))
                    .verifyComplete();

            assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
            verify(chain, never()).filter(any());
        }

        @Test
        @DisplayName("deve retornar 401 quando token está expirado")
        void shouldReturn401WhenTokenIsExpired() {
            when(jwtUtil.extractClaims("token-expirado"))
                    .thenThrow(new io.jsonwebtoken.ExpiredJwtException(null, null, "Token expirado"));

            MockServerWebExchange exchange = MockServerWebExchange.from(
                    MockServerHttpRequest.get("/payments/user")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer token-expirado")
                            .build()
            );

            StepVerifier.create(filter.filter(exchange, chain))
                    .verifyComplete();

            assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
            verify(chain, never()).filter(any());
        }
    }

    // -------------------------------------------------------------------------
    // Token válido — propagação de headers
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("token válido")
    class ValidToken {

        @Test
        @DisplayName("deve propagar X-Auth-Username e X-Auth-Role para o request downstream")
        void shouldPropagateAuthHeadersDownstream() {
            Claims claims = mock(Claims.class);
            when(jwtUtil.extractClaims("token-valido")).thenReturn(claims);
            when(jwtUtil.extractUsername(claims)).thenReturn("user@example.com");
            when(jwtUtil.extractRole(claims)).thenReturn("USER");

            ServerWebExchange[] capturedExchange = new ServerWebExchange[1];
            when(chain.filter(any())).thenAnswer(inv -> {
                capturedExchange[0] = inv.getArgument(0);
                return Mono.empty();
            });

            MockServerWebExchange exchange = MockServerWebExchange.from(
                    MockServerHttpRequest.get("/orders")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer token-valido")
                            .build()
            );

            StepVerifier.create(filter.filter(exchange, chain))
                    .verifyComplete();

            verify(chain).filter(any());
            assertThat(capturedExchange[0].getRequest().getHeaders().getFirst("X-Auth-Username"))
                    .isEqualTo("user@example.com");
            assertThat(capturedExchange[0].getRequest().getHeaders().getFirst("X-Auth-Role"))
                    .isEqualTo("USER");
        }

        @Test
        @DisplayName("deve propagar role ADMIN corretamente")
        void shouldPropagateAdminRole() {
            Claims claims = mock(Claims.class);
            when(jwtUtil.extractClaims("token-admin")).thenReturn(claims);
            when(jwtUtil.extractUsername(claims)).thenReturn("admin@example.com");
            when(jwtUtil.extractRole(claims)).thenReturn("ADMIN");

            ServerWebExchange[] capturedExchange = new ServerWebExchange[1];
            when(chain.filter(any())).thenAnswer(inv -> {
                capturedExchange[0] = inv.getArgument(0);
                return Mono.empty();
            });

            MockServerWebExchange exchange = MockServerWebExchange.from(
                    MockServerHttpRequest.get("/orders/all")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer token-admin")
                            .build()
            );

            StepVerifier.create(filter.filter(exchange, chain))
                    .verifyComplete();

            assertThat(capturedExchange[0].getRequest().getHeaders().getFirst("X-Auth-Role"))
                    .isEqualTo("ADMIN");
        }

        @Test
        @DisplayName("não deve modificar o response quando token é válido")
        void shouldNotSetResponseStatusForValidToken() {
            Claims claims = mock(Claims.class);
            when(jwtUtil.extractClaims("token-valido")).thenReturn(claims);
            when(jwtUtil.extractUsername(claims)).thenReturn("user@example.com");
            when(jwtUtil.extractRole(claims)).thenReturn("USER");
            when(chain.filter(any())).thenReturn(Mono.empty());

            MockServerWebExchange exchange = MockServerWebExchange.from(
                    MockServerHttpRequest.get("/catalog/products")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer token-valido")
                            .build()
            );

            StepVerifier.create(filter.filter(exchange, chain))
                    .verifyComplete();

            assertThat(exchange.getResponse().getStatusCode()).isNull();
        }
    }

    // -------------------------------------------------------------------------
    // getOrder
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("getOrder")
    class GetOrder {

        @Test
        @DisplayName("deve ter ordem -1 para executar antes dos outros filtros")
        void shouldHaveOrderMinusOne() {
            assertThat(filter.getOrder()).isEqualTo(-1);
        }
    }
}