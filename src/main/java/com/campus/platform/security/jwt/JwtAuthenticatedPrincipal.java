package com.campus.platform.security.jwt;

import com.campus.platform.common.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.UUID;

/**
 * Immutable principal object stored in the Spring SecurityContext after JWT validation.
 *
 * Usage in controllers:
 *   JwtAuthenticatedPrincipal principal =
 *       (JwtAuthenticatedPrincipal) SecurityContextHolder
 *           .getContext().getAuthentication().getPrincipal();
 *
 * Or inject via @AuthenticationPrincipal if configured.
 */
@Getter
@AllArgsConstructor
public class JwtAuthenticatedPrincipal {

    private final UUID userId;
    private final String email;
    private final UserRole role;
    private final UUID collegeId; // null for SUPER_ADMIN
}
