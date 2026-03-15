package com.auth.service.service;

import com.auth.service.domain.User;
import com.auth.service.dto.AuthResponse;
import com.auth.service.dto.LoginRequest;
import com.auth.service.dto.RegisterRequest;
import com.auth.service.dto.UserResponse;
import com.auth.service.dto.events.UserRegisteredEvent;
import com.auth.service.exception.EmailAlreadyExistsException;
import com.auth.service.exception.InvalidCredentialsException;
import com.auth.service.messaging.AuthEventPublisher;
import com.auth.service.repository.UserRepository;
import org.slf4j.Logger;import org.slf4j.LoggerFactory;
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
    private final AuthEventPublisher eventPublisher;

    public AuthService(UserRepository userRepository,
                       JwtService jwtService,
                       PasswordEncoder passwordEncoder,
                       AuthEventPublisher eventPublisher) {
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            log.warn("Tentativa de registro com e-mail já existente: {}", request.getEmail());
            throw new EmailAlreadyExistsException(request.getEmail());
        }

        User user = new User();
        user.setEmail(request.getEmail());
        user.setName(request.getName());
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        User saved = userRepository.save(user);

        eventPublisher.publishUserRegistered(new UserRegisteredEvent(
                saved.getId(),
                saved.getEmail(),
                saved.getName()
        ));

        log.info("Novo usuário registrado: id={}, email={}", saved.getId(), saved.getEmail());

        String token = jwtService.generateToken(saved);
        return new AuthResponse(token, saved.getEmail(), saved.getName(), saved.getRole().name());
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> {
                    log.warn("Tentativa de login com e-mail não cadastrado: {}", request.getEmail());
                    return new InvalidCredentialsException();
                });

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            log.warn("Senha incorreta para o usuário: id={}, email={}", user.getId(), user.getEmail());
            throw new InvalidCredentialsException();
        }

        log.info("Login bem-sucedido: id={}, email={}, role={}",
                user.getId(), user.getEmail(), user.getRole());

        String token = jwtService.generateToken(user);
        return new AuthResponse(token, user.getEmail(), user.getName(), user.getRole().name());
    }

    public Page<UserResponse> findAll(Pageable pageable) {
        return userRepository.findAll(pageable)
                .map(u -> new UserResponse(u.getId(), u.getName(), u.getEmail(), u.getRole().name()));
    }
}