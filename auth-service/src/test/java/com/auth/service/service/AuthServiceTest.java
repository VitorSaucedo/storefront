package com.auth.service.service;

import com.auth.service.domain.Role;
import com.auth.service.domain.User;
import com.auth.service.dto.AuthResponse;
import com.auth.service.dto.LoginRequest;
import com.auth.service.dto.RegisterRequest;
import com.auth.service.dto.UserResponse;
import com.auth.service.dto.events.UserRegisteredEvent;
import com.auth.service.exception.EmailAlreadyExistsException;
import com.auth.service.exception.InvalidCredentialsException;
import com.auth.service.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtService jwtService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private AuthService authService;

    private User mockUser;

    @BeforeEach
    void setUp() {
        mockUser = new User();
        mockUser.setId(1L);
        mockUser.setEmail("user@example.com");
        mockUser.setName("John Doe");
        mockUser.setPassword("hashed-password");
        mockUser.setRole(Role.USER);
    }

    // -------------------------------------------------------------------------
    // register()
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("register()")
    class Register {

        @Test
        @DisplayName("deve registrar usuário com sucesso e retornar AuthResponse com token")
        void shouldRegisterUserAndReturnAuthResponse() {
            RegisterRequest request = RegisterRequest.builder()
                    .name("John Doe")
                    .email("user@example.com")
                    .password("password123")
                    .build();

            when(userRepository.existsByEmail(request.email())).thenReturn(false); // Acesso via record: email()
            when(passwordEncoder.encode(request.password())).thenReturn("hashed-password");
            when(userRepository.save(any(User.class))).thenReturn(mockUser);
            when(jwtService.generateToken(mockUser)).thenReturn("jwt-token");

            AuthResponse response = authService.register(request);

            assertThat(response).isNotNull();
            assertThat(response.token()).isEqualTo("jwt-token");
            assertThat(response.email()).isEqualTo("user@example.com");
            assertThat(response.name()).isEqualTo("John Doe");
            assertThat(response.role()).isEqualTo("USER");
        }

        @Test
        @DisplayName("deve salvar usuário com senha encodada (nunca em texto puro)")
        void shouldSaveUserWithEncodedPassword() {
            RegisterRequest request = RegisterRequest.builder()
                    .name("John Doe")
                    .email("user@example.com")
                    .password("password123")
                    .build();

            when(userRepository.existsByEmail(anyString())).thenReturn(false);
            when(passwordEncoder.encode("password123")).thenReturn("$2a$10$hashed");
            when(userRepository.save(any(User.class))).thenReturn(mockUser);
            when(jwtService.generateToken(any())).thenReturn("jwt-token");

            authService.register(request);

            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());
            assertThat(userCaptor.getValue().getPassword()).isEqualTo("$2a$10$hashed");
            assertThat(userCaptor.getValue().getPassword()).isNotEqualTo("password123");
        }

        @Test
        @DisplayName("deve publicar evento UserRegisteredEvent após salvar o usuário")
        void shouldPublishUserRegisteredEventAfterSave() {
            RegisterRequest request = RegisterRequest.builder()
                    .name("John Doe")
                    .email("user@example.com")
                    .password("password123")
                    .build();

            when(userRepository.existsByEmail(anyString())).thenReturn(false);
            when(passwordEncoder.encode(anyString())).thenReturn("hashed-password");
            when(userRepository.save(any(User.class))).thenReturn(mockUser);
            when(jwtService.generateToken(any())).thenReturn("jwt-token");

            authService.register(request);

            ArgumentCaptor<UserRegisteredEvent> eventCaptor = ArgumentCaptor.forClass(UserRegisteredEvent.class);
            verify(eventPublisher).publishEvent(eventCaptor.capture());

            UserRegisteredEvent event = eventCaptor.getValue();
            assertThat(event.userId()).isEqualTo(1L);
            assertThat(event.email()).isEqualTo("user@example.com");
            assertThat(event.name()).isEqualTo("John Doe");
        }

        @Test
        @DisplayName("deve lançar EmailAlreadyExistsException quando e-mail já está cadastrado")
        void shouldThrowEmailAlreadyExistsExceptionWhenEmailIsTaken() {
            RegisterRequest request = RegisterRequest.builder()
                    .name("John Doe")
                    .email("user@example.com")
                    .password("password123")
                    .build();

            when(userRepository.existsByEmail(request.email())).thenReturn(true);

            assertThatThrownBy(() -> authService.register(request))
                    .isInstanceOf(EmailAlreadyExistsException.class);

            verify(userRepository, never()).save(any());
            verify(eventPublisher, never()).publishEvent(any(UserRegisteredEvent.class));
            verify(jwtService, never()).generateToken(any());
        }

        @Test
        @DisplayName("não deve publicar evento se o save lançar exceção")
        void shouldNotPublishEventWhenSaveFails() {
            RegisterRequest request = RegisterRequest.builder()
                    .name("John Doe")
                    .email("user@example.com")
                    .password("password123")
                    .build();

            when(userRepository.existsByEmail(anyString())).thenReturn(false);
            when(passwordEncoder.encode(anyString())).thenReturn("hashed-password");
            when(userRepository.save(any(User.class))).thenThrow(new RuntimeException("DB error"));

            assertThatThrownBy(() -> authService.register(request))
                    .isInstanceOf(RuntimeException.class);

            verify(eventPublisher, never()).publishEvent(any(UserRegisteredEvent.class));
        }
    }

    // -------------------------------------------------------------------------
    // login()
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("login()")
    class Login {

        @Test
        @DisplayName("deve retornar AuthResponse com token ao fazer login com credenciais corretas")
        void shouldReturnAuthResponseWhenCredentialsAreValid() {
            LoginRequest request = LoginRequest.builder()
                    .email("user@example.com")
                    .password("password123")
                    .build();

            when(userRepository.findByEmail(request.email())).thenReturn(Optional.of(mockUser));
            when(passwordEncoder.matches("password123", "hashed-password")).thenReturn(true);
            when(jwtService.generateToken(mockUser)).thenReturn("jwt-token");

            AuthResponse response = authService.login(request);

            assertThat(response).isNotNull();
            assertThat(response.token()).isEqualTo("jwt-token");
            assertThat(response.email()).isEqualTo("user@example.com");
            assertThat(response.role()).isEqualTo("USER");
        }

        @Test
        @DisplayName("deve lançar InvalidCredentialsException quando e-mail não está cadastrado")
        void shouldThrowInvalidCredentialsWhenEmailNotFound() {
            LoginRequest request = LoginRequest.builder()
                    .email("unknown@example.com")
                    .password("password123")
                    .build();

            when(userRepository.findByEmail(request.email())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(InvalidCredentialsException.class);

            verify(passwordEncoder, never()).matches(anyString(), anyString());
            verify(jwtService, never()).generateToken(any());
        }

        @Test
        @DisplayName("deve lançar InvalidCredentialsException quando a senha está errada")
        void shouldThrowInvalidCredentialsWhenPasswordIsWrong() {
            LoginRequest request = LoginRequest.builder()
                    .email("user@example.com")
                    .password("wrong-password")
                    .build();

            when(userRepository.findByEmail(request.email())).thenReturn(Optional.of(mockUser));
            when(passwordEncoder.matches("wrong-password", "hashed-password")).thenReturn(false);

            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(InvalidCredentialsException.class);

            verify(jwtService, never()).generateToken(any());
        }

        @Test
        @DisplayName("não deve expor se o erro é no e-mail ou na senha (mesmo tipo de exceção)")
        void shouldThrowSameExceptionForEmailAndPasswordErrors() {
            LoginRequest badEmail = LoginRequest.builder()
                    .email("notfound@example.com")
                    .password("anypass")
                    .build();

            LoginRequest badPassword = LoginRequest.builder()
                    .email("user@example.com")
                    .password("wrongpass")
                    .build();

            when(userRepository.findByEmail("notfound@example.com")).thenReturn(Optional.empty());
            when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(mockUser));
            when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

            assertThatThrownBy(() -> authService.login(badEmail))
                    .isExactlyInstanceOf(InvalidCredentialsException.class);
            assertThatThrownBy(() -> authService.login(badPassword))
                    .isExactlyInstanceOf(InvalidCredentialsException.class);
        }
    }

    // -------------------------------------------------------------------------
    // findAll()
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("findAll()")
    class FindAll {

        @Test
        @DisplayName("deve retornar página de UserResponse mapeada corretamente")
        void shouldReturnMappedPageOfUserResponse() {
            Pageable pageable = PageRequest.of(0, 20);
            Page<User> userPage = new PageImpl<>(List.of(mockUser), pageable, 1);
            when(userRepository.findAll(pageable)).thenReturn(userPage);

            Page<UserResponse> result = authService.findAll(pageable);

            assertThat(result).isNotNull();
            assertThat(result.getTotalElements()).isEqualTo(1);
            UserResponse dto = result.getContent().get(0);
            assertThat(dto.id()).isEqualTo(1L);
            assertThat(dto.name()).isEqualTo("John Doe");
            assertThat(dto.email()).isEqualTo("user@example.com");
            assertThat(dto.role()).isEqualTo("USER");
        }

        @Test
        @DisplayName("deve retornar página vazia quando não há usuários")
        void shouldReturnEmptyPageWhenNoUsers() {
            Pageable pageable = PageRequest.of(0, 20);
            when(userRepository.findAll(pageable)).thenReturn(Page.empty(pageable));

            Page<UserResponse> result = authService.findAll(pageable);

            assertThat(result).isNotNull();
            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isZero();
        }

        @Test
        @DisplayName("deve repassar o Pageable recebido ao repository sem modificação")
        void shouldPassPageableToRepositoryAsIs() {
            Pageable pageable = PageRequest.of(2, 10);
            when(userRepository.findAll(pageable)).thenReturn(Page.empty(pageable));

            authService.findAll(pageable);

            verify(userRepository).findAll(pageable);
        }

        @Test
        @DisplayName("deve mapear role do usuário para String no DTO")
        void shouldMapUserRoleToStringInDto() {
            mockUser.setRole(Role.ADMIN);
            Pageable pageable = PageRequest.of(0, 20);
            when(userRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(mockUser)));

            Page<UserResponse> result = authService.findAll(pageable);

            assertThat(result.getContent().get(0).role()).isEqualTo("ADMIN");
        }
    }
}