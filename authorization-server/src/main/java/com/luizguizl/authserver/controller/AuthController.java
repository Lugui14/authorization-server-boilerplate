package com.luizguizl.authserver.controller;

import com.luizguizl.authserver.service.AuthService;
import com.luizguizl.common.dto.ApiResponse;
import com.luizguizl.common.dto.LoginRequest;
import com.luizguizl.common.dto.TokenResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<TokenResponse>> login(@Valid @RequestBody LoginRequest loginRequest) {
        try {
            TokenResponse tokenResponse = authService.authenticate(loginRequest);

            return ResponseEntity.ok(ApiResponse.<TokenResponse>builder()
                    .success(true)
                    .message("Login successful")
                    .data(tokenResponse)
                    .build());

        } catch (BadCredentialsException e) {
            log.error("Login failed for user: {}", loginRequest.getEmail());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.<TokenResponse>builder()
                            .success(false)
                            .message("Invalid credentials")
                            .error(e.getMessage())
                            .build());
        }
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCurrentUser(Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.<Map<String, Object>>builder()
                            .success(false)
                            .message("Not authenticated")
                            .build());
        }

        Map<String, Object> userInfo = authService.getUserInfo(authentication);

        return ResponseEntity.ok(ApiResponse.<Map<String, Object>>builder()
                .success(true)
                .message("User information retrieved successfully")
                .data(userInfo)
                .build());
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout() {
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("Logout successful")
                .build());
    }
}

