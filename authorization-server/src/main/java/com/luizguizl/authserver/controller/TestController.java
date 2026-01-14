package com.luizguizl.authserver.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
public class TestController {

    private final PasswordEncoder passwordEncoder;

    @GetMapping("/encode")
    public Map<String, String> encodePassword(@RequestParam String password) {
        Map<String, String> result = new HashMap<>();
        result.put("password", password);
        result.put("encoded", passwordEncoder.encode(password));
        return result;
    }

    @GetMapping("/verify")
    public Map<String, Object> verifyPassword(@RequestParam String raw, @RequestParam String encoded) {
        Map<String, Object> result = new HashMap<>();
        result.put("raw", raw);
        result.put("encoded", encoded);
        result.put("matches", passwordEncoder.matches(raw, encoded));
        return result;
    }
}

