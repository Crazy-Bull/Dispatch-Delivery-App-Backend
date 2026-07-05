package com.laioffer.dispatchdeliveryapp.controller;

import com.laioffer.dispatchdeliveryapp.dto.AuthResponse;
import com.laioffer.dispatchdeliveryapp.dto.LoginRequest;
import com.laioffer.dispatchdeliveryapp.dto.SignUpRequest;
import com.laioffer.dispatchdeliveryapp.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/signup")
    public ResponseEntity<AuthResponse> signUp(@RequestBody SignUpRequest request) {
        AuthResponse response = authService.signUp(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }
}
