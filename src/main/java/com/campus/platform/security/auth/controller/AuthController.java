package com.campus.platform.security.auth.controller;

import com.campus.platform.security.auth.dto.AuthResponseDto;
import com.campus.platform.security.auth.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponseDto> refresh(
            @RequestHeader("X-Refresh-Token") String refreshToken) {
        log.info("=== /auth/refresh HIT ===");
        log.info("X-Refresh-Token header present: {}", refreshToken != null);
        return ResponseEntity.ok(authService.refresh(refreshToken));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @RequestHeader("X-Refresh-Token") String refreshToken,
            HttpServletResponse response) {
        authService.logout(refreshToken);
        return ResponseEntity.noContent().build();
    }
}