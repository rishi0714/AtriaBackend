package com.campus.platform.bootstrap;

import com.campus.platform.common.enums.UserRole;
import com.campus.platform.user.entity.User;
import com.campus.platform.user.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PlatformOwnerBootstrap {

    private final UserRepository userRepository;

    @Value("${app.platform-owner-email}")
    private String email;

    @Value("${app.platform-owner-name}")
    private String name;

    @PostConstruct
    public void init() {
        if (email == null || email.isBlank()) {
            log.warn("app.platform-owner-email not configured.");
            return;
        }

        userRepository.findByEmail(email.toLowerCase()).ifPresentOrElse(
                existing -> {
                    if (existing.getRole() != UserRole.PLATFORM_OWNER) {
                        existing.setRole(UserRole.PLATFORM_OWNER);
                        existing.setCollege(null);
                        userRepository.save(existing);
                        log.info("Bootstrap: promoted {} to PLATFORM_OWNER", email);
                    } else {
                        log.info("Bootstrap: {} is already PLATFORM_OWNER", email);
                    }
                },
                () -> log.info("Bootstrap: {} not in DB yet — will be promoted on first Google login.", email)
        );
    }
}