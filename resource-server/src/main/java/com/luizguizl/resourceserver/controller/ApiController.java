package com.luizguizl.resourceserver.controller;

import com.luizguizl.common.dto.ApiResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ApiController {

    @GetMapping("/public/hello")
    public ApiResponse<String> publicHello() {
        return ApiResponse.<String>builder()
                .success(true)
                .message("This is a public endpoint")
                .data("Hello, World!")
                .build();
    }

    @GetMapping("/user/profile")
    @PreAuthorize("hasRole('USER')")
    public ApiResponse<Map<String, Object>> getUserProfile(@AuthenticationPrincipal Jwt jwt) {
        Map<String, Object> profile = new HashMap<>();
        profile.put("subject", jwt.getSubject());
        profile.put("email", jwt.getClaim("email"));
        profile.put("name", jwt.getClaim("name"));
        profile.put("roles", jwt.getClaim("scope"));

        return ApiResponse.<Map<String, Object>>builder()
                .success(true)
                .message("User profile retrieved successfully")
                .data(profile)
                .build();
    }

    @GetMapping("/user/data")
    @PreAuthorize("hasRole('USER')")
    public ApiResponse<String> getUserData(@AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.<String>builder()
                .success(true)
                .message("Protected data accessed successfully")
                .data("Hello, " + jwt.getClaim("name") + "! This is protected data.")
                .build();
    }

    @GetMapping("/admin/dashboard")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<String> getAdminDashboard(@AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.<String>builder()
                .success(true)
                .message("Admin dashboard accessed successfully")
                .data("Welcome to admin dashboard, " + jwt.getClaim("name"))
                .build();
    }

    @GetMapping("/health")
    public ApiResponse<String> health() {
        return ApiResponse.<String>builder()
                .success(true)
                .message("Resource server is healthy")
                .data("OK")
                .build();
    }
}

