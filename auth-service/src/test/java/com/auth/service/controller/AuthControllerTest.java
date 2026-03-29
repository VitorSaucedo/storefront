package com.auth.service.controller;

import com.auth.service.config.TestSecurityConfig;
import com.auth.service.dto.AuthResponse;
import com.auth.service.dto.LoginRequest;
import com.auth.service.dto.RegisterRequest;
import com.auth.service.dto.UserResponse;
import com.auth.service.exception.EmailAlreadyExistsException;
import com.auth.service.exception.InvalidCredentialsException;
import com.auth.service.service.AuthService;
import com.auth.service.service.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@Import(TestSecurityConfig.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private JwtService jwtService;

    private AuthResponse authResponse;

    @BeforeEach
    void setUp() {
        authResponse = AuthResponse.builder()
                .token("mocked-jwt-token")
                .email("user@example.com")
                .name("John Doe")
                .role("USER")
                .build();
    }

    // -------------------------------------------------------------------------
    // POST /auth/register
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("POST /auth/register")
    class Register {

        @Test
        @DisplayName("deve retornar 201 Created com token ao registrar usuário válido")
        void shouldReturn201WhenRegisterIsSuccessful() throws Exception {
            RegisterRequest request = RegisterRequest.builder()
                    .name("John Doe")
                    .email("user@example.com")
                    .password("password123")
                    .build();

            when(authService.register(any(RegisterRequest.class))).thenReturn(authResponse);

            mockMvc.perform(post("/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.token").value("mocked-jwt-token"))
                    .andExpect(jsonPath("$.email").value("user@example.com"))
                    .andExpect(jsonPath("$.name").value("John Doe"))
                    .andExpect(jsonPath("$.role").value("USER"));
        }

        @Test
        @DisplayName("deve retornar 400 Bad Request quando o body está vazio")
        void shouldReturn400WhenBodyIsEmpty() throws Exception {
            mockMvc.perform(post("/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("deve retornar 400 Bad Request quando email é inválido")
        void shouldReturn400WhenEmailIsInvalid() throws Exception {
            RegisterRequest request = RegisterRequest.builder()
                    .name("John Doe")
                    .email("not-an-email")
                    .password("password123")
                    .build();

            mockMvc.perform(post("/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("deve retornar 409 Conflict quando e-mail já está cadastrado")
        void shouldReturn409WhenEmailAlreadyExists() throws Exception {
            RegisterRequest request = RegisterRequest.builder()
                    .name("John Doe")
                    .email("user@example.com")
                    .password("password123")
                    .build();

            when(authService.register(any(RegisterRequest.class)))
                    .thenThrow(new EmailAlreadyExistsException("user@example.com"));

            mockMvc.perform(post("/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict());
        }
    }

    // -------------------------------------------------------------------------
    // POST /auth/login
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("POST /auth/login")
    class Login {

        @Test
        @DisplayName("deve retornar 200 OK com token ao realizar login com credenciais válidas")
        void shouldReturn200WhenLoginIsSuccessful() throws Exception {
            LoginRequest request = LoginRequest.builder()
                    .email("user@example.com")
                    .password("password123")
                    .build();

            when(authService.login(any(LoginRequest.class))).thenReturn(authResponse);

            mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").value("mocked-jwt-token"))
                    .andExpect(jsonPath("$.email").value("user@example.com"));
        }

        @Test
        @DisplayName("deve retornar 400 Bad Request quando o body está vazio")
        void shouldReturn400WhenBodyIsEmpty() throws Exception {
            mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("deve retornar 401 Unauthorized quando as credenciais são inválidas")
        void shouldReturn401WhenCredentialsAreInvalid() throws Exception {
            LoginRequest request = LoginRequest.builder()
                    .email("user@example.com")
                    .password("wrongpassword")
                    .build();

            when(authService.login(any(LoginRequest.class)))
                    .thenThrow(new InvalidCredentialsException());

            mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }
    }

    // -------------------------------------------------------------------------
    // GET /auth/users
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("GET /auth/users")
    class FindAll {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("deve retornar 200 OK com página de usuários quando autenticado como ADMIN")
        void shouldReturn200WithUserPageWhenAdminAuthenticated() throws Exception {
            UserResponse userResponse = UserResponse.builder()
                    .id(1L)
                    .name("John Doe")
                    .email("user@example.com")
                    .role("USER")
                    .build();

            Page<UserResponse> page = new PageImpl<>(List.of(userResponse), PageRequest.of(0, 20), 1);
            when(authService.findAll(any())).thenReturn(page);

            mockMvc.perform(get("/auth/users"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].id").value(1L))
                    .andExpect(jsonPath("$.content[0].email").value("user@example.com"))
                    .andExpect(jsonPath("$.totalElements").value(1));
        }

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("deve retornar 403 Forbidden quando autenticado como USER comum")
        void shouldReturn403WhenAuthenticatedAsRegularUser() throws Exception {
            mockMvc.perform(get("/auth/users"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("deve retornar 401 Unauthorized quando não autenticado")
        void shouldReturn401WhenNotAuthenticated() throws Exception {
            mockMvc.perform(get("/auth/users"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("deve retornar página vazia quando não há usuários cadastrados")
        void shouldReturnEmptyPageWhenNoUsersExist() throws Exception {
            Page<UserResponse> emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);
            when(authService.findAll(any())).thenReturn(emptyPage);

            mockMvc.perform(get("/auth/users"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isEmpty())
                    .andExpect(jsonPath("$.totalElements").value(0));
        }
    }
}