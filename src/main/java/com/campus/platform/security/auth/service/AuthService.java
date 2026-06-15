package com.campus.platform.security.auth.service;

import com.campus.platform.security.auth.dto.AuthResponseDto;
import com.campus.platform.security.jwt.JwtTokenProvider;
import com.campus.platform.user.entity.User;
import com.campus.platform.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;

    @Transactional
    public AuthResponseDto refresh(String refreshToken) {

        log.info("=== REFRESH CALLED ===");
        log.info("Received refreshToken: {}",
                refreshToken != null ? refreshToken.substring(0, 20) + "..." : "NULL");
        if (!jwtTokenProvider.validateRefreshToken(refreshToken)) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "Invalid or expired refresh token");
        }

        UUID userId = jwtTokenProvider.extractUserIdFromRefreshToken(refreshToken);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "User not found"));

        if (!refreshToken.equals(user.getRefreshToken())) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "Refresh token mismatch");
        }

        String newAccessToken = jwtTokenProvider.generateToken(
                user.getUserId(),
                user.getEmail(),
                user.getRole(),         // ← fresh role from DB
                user.getCollege() != null ? user.getCollege().getCollegeId() : null
        );

        log.info("Access token refreshed — userId: {}, role: {}", user.getUserId(), user.getRole());

        return AuthResponseDto.builder()
                .token(newAccessToken)
                .userId(user.getUserId().toString())
                .email(user.getEmail())
                .role(user.getRole().name())
                .collegeId(user.getCollege() != null
                        ? user.getCollege().getCollegeId().toString() : null)
                .build();
    }

    @Transactional
    public void logout(String refreshToken) {
        userRepository.findByRefreshToken(refreshToken)
                .ifPresent(user -> {
                    user.setRefreshToken(null);
                    userRepository.save(user);
                    log.info("Refresh token cleared — userId: {}", user.getUserId());
                });
    }
}