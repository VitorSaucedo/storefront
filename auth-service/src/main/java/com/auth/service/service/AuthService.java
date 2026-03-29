package com.auth.service.service;

import com.auth.service.domain.User;
import com.auth.service.dto.AuthResponse;
import com.auth.service.dto.LoginRequest;
import com.auth.service.dto.RegisterRequest;
import com.auth.service.dto.UserResponse;
import com.auth.service.dto.events.UserRegisteredEvent;
import com.auth.service.exception.EmailAlreadyExistsException;
import com.auth.service.exception.InvalidCredentialsException;
import com.auth.service.repository.UserRepository;
import org.slf4j.Logger;import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Pageable;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher eventPublisher;

    public AuthService(UserRepository userRepository,
                       JwtService jwtService,
                       PasswordEncoder passwordEncoder,
                       ApplicationEventPublisher eventPublisher) {
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            log.warn("Tentativa de registro com e-mail já existente: {}", request.email());
            throw new EmailAlreadyExistsException(request.email());
        }

        User user = new User();
        user.setEmail(request.email());
        user.setName(request.name());
        user.setPassword(passwordEncoder.encode(request.password()));

        User saved = userRepository.save(user);

        eventPublisher.publishEvent(UserRegisteredEvent.builder()
                .userId(saved.getId())
                .email(saved.getEmail())
                .name(saved.getName())
                .occurredAt(java.time.LocalDateTime.now())
                .build());

        log.info("Novo usuário registrado: id={}, email={}", saved.getId(), saved.getEmail());

        String token = jwtService.generateToken(saved);

        return AuthResponse.builder()
                .token(token)
                .email(saved.getEmail())
                .name(saved.getName())
                .role(saved.getRole().name())
                .build();
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> {
                    log.warn("Tentativa de login com e-mail não cadastrado: {}", request.email());
                    return new InvalidCredentialsException();
                });

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            log.warn("Senha incorreta para o usuário: id={}, email={}", user.getId(), user.getEmail());
            throw new InvalidCredentialsException();
        }

        log.info("Login bem-sucedido: id={}, email={}, role={}",
                user.getId(), user.getEmail(), user.getRole());

        String token = jwtService.generateToken(user);

        return AuthResponse.builder()
                .token(token)
                .email(user.getEmail())
                .name(user.getName())
                .role(user.getRole().name())
                .build();
    }

    public Page<UserResponse> findAll(Pageable pageable) {
        return userRepository.findAll(pageable)
                .map(u -> UserResponse.builder()
                        .id(u.getId())
                        .name(u.getName())
                        .email(u.getEmail())
                        .role(u.getRole().name())
                        .build());
    }
}