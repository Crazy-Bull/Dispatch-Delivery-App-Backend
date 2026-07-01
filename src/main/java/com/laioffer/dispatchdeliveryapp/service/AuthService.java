package com.laioffer.dispatchdeliveryapp.service;

import com.laioffer.dispatchdeliveryapp.dto.AuthResponse;
import com.laioffer.dispatchdeliveryapp.dto.LoginRequest;
import com.laioffer.dispatchdeliveryapp.dto.SignUpRequest;
import com.laioffer.dispatchdeliveryapp.dto.UserResponse;
import com.laioffer.dispatchdeliveryapp.entity.User;
import com.laioffer.dispatchdeliveryapp.repository.UserRepository;
import com.laioffer.dispatchdeliveryapp.security.JwtService;
import com.laioffer.dispatchdeliveryapp.util.UserMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse signUp(SignUpRequest request) {
        validateSignUpInput(request);

        if (userRepository.findByEmail(request.email()).isPresent()) {
            throw new IllegalArgumentException("Email already exists: " + request.email());
        }

        String passwordHash = passwordEncoder.encode(request.password());
        User saved = userRepository.save(new User(
                null, request.name(), request.address(), request.email(), passwordHash));

        UserResponse userResponse = UserMapper.toResponse(saved);
        String token = jwtService.generateToken(saved.id(), saved.email());
        return new AuthResponse(token, userResponse);
    }

    public AuthResponse login(LoginRequest request) {
        if (request.email() == null || request.email().isBlank()) {
            throw new IllegalArgumentException("Email is required");
        }
        if (request.password() == null || request.password().isBlank()) {
            throw new IllegalArgumentException("Password is required");
        }

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));

        if (!passwordEncoder.matches(request.password(), user.passwordHash())) {
            throw new IllegalArgumentException("Invalid email or password");
        }

        UserResponse userResponse = UserMapper.toResponse(user);
        String token = jwtService.generateToken(user.id(), user.email());
        return new AuthResponse(token, userResponse);
    }

    private void validateSignUpInput(SignUpRequest request) {
        if (request.name() == null || request.name().isBlank()) {
            throw new IllegalArgumentException("Name is required");
        }
        if (request.address() == null || request.address().isBlank()) {
            throw new IllegalArgumentException("Address is required");
        }
        if (request.email() == null || request.email().isBlank()) {
            throw new IllegalArgumentException("Email is required");
        }
        if (request.password() == null || request.password().length() < 6) {
            throw new IllegalArgumentException("Password must be at least 6 characters");
        }
    }
}
