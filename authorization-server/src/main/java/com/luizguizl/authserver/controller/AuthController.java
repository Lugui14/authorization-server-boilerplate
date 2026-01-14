package com.luizguizl.authserver.controller;

import com.luizguizl.authserver.entity.User;
import com.luizguizl.authserver.repository.UserRepository;
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
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtEncoder jwtEncoder;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<TokenResponse>> login(@Valid @RequestBody LoginRequest loginRequest) {
        try {
            log.info("Login attempt for user: {}", loginRequest.getEmail());

            User user = userRepository.findByEmail(loginRequest.getEmail())
                    .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

            if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
                throw new BadCredentialsException("Invalid credentials");
            }

            if (!user.isEnabled()) {
                throw new BadCredentialsException("Account is disabled");
            }

            String token = generateToken(user);

            TokenResponse tokenResponse = TokenResponse.builder()
                    .accessToken(token)
                    .tokenType("Bearer")
                    .expiresIn(3600)
                    .scope(String.join(" ", user.getRoles()))
                    .build();

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

        Map<String, Object> userInfo = new HashMap<>();

        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            userInfo.put("subject", jwtAuth.getToken().getSubject());
            userInfo.put("email", jwtAuth.getToken().getClaim("email"));
            userInfo.put("name", jwtAuth.getToken().getClaim("name"));
            userInfo.put("roles", jwtAuth.getToken().getClaim("scope"));
        } else {
            userInfo.put("username", authentication.getName());
            userInfo.put("authorities", authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.toList()));
        }

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

    private String generateToken(User user) {
        Instant now = Instant.now();
        long expiry = 3600L;

        String scope = user.getRoles().stream()
                .map(role -> "ROLE_" + role)
                .collect(Collectors.joining(" "));

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("http://localhost:8080")
                .issuedAt(now)
                .expiresAt(now.plusSeconds(expiry))
                .subject(user.getEmail())
                .claim("email", user.getEmail())
                .claim("name", user.getName())
                .claim("scope", scope)
                .build();

        return this.jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }
}

