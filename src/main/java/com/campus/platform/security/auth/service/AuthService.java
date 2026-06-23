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

        if (refreshToken == null || refreshToken.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Refresh token missing");
        }

        log.info("Refresh token length: {}", refreshToken.length());

        if (!jwtTokenProvider.validateRefreshToken(refreshToken)) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Invalid or expired refresh token");
        }

        UUID userId = jwtTokenProvider.extractUserIdFromRefreshToken(refreshToken);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "User not found"));

        if (!refreshToken.equals(user.getRefreshToken())) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "Refresh token mismatch");
        }

        var college = user.getCollege();
        UUID collegeId = college != null ? college.getCollegeId() : null;

        String newAccessToken = jwtTokenProvider.generateToken(
                user.getUserId(),
                user.getEmail(),
                user.getRole(),
                collegeId
        );

        log.info("Access token refreshed — userId: {}, role: {}", user.getUserId(), user.getRole());

        return AuthResponseDto.builder()
                .token(newAccessToken)
                .userId(user.getUserId().toString())
                .email(user.getEmail())
                .role(user.getRole().name())
                .collegeId(collegeId != null ? collegeId.toString() : null)
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