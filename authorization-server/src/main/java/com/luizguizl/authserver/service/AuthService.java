package com.luizguizl.authserver.service;

import com.luizguizl.authserver.config.AppProperties;
import com.luizguizl.authserver.entity.User;
import com.luizguizl.authserver.repository.UserRepository;
import com.luizguizl.common.dto.LoginRequest;
import com.luizguizl.common.dto.TokenResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtEncoder jwtEncoder;
    private final AppProperties appProperties;

    public TokenResponse authenticate(LoginRequest loginRequest) {
        log.info("Authentication attempt for user: {}", loginRequest.getEmail());

        User user = userRepository.findByEmail(loginRequest.getEmail())
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
            throw new BadCredentialsException("Invalid credentials");
        }

        if (!user.isEnabled()) {
            throw new BadCredentialsException("Account is disabled");
        }

        String token = generateToken(user);

        log.info("Authentication successful for user: {}", loginRequest.getEmail());

        return TokenResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .expiresIn(appProperties.getJwt().getAccessTokenExpirationSeconds())
                .scope(String.join(" ", user.getRoles()))
                .build();
    }

    public Map<String, Object> getUserInfo(Authentication authentication) {
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

        return userInfo;
    }

    private String generateToken(User user) {
        Instant now = Instant.now();
        long expiry = appProperties.getJwt().getAccessTokenExpirationSeconds();

        String scope = user.getRoles().stream()
                .map(role -> "ROLE_" + role)
                .collect(Collectors.joining(" "));

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(appProperties.getJwt().getIssuer())
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

