package com.campus.platform.common.util;

import com.campus.platform.exception.TenantAccessDeniedException;
import com.campus.platform.security.jwt.JwtAuthenticatedPrincipal;
import com.campus.platform.security.oauth2.CustomOAuth2User;
import com.campus.platform.user.entity.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

public final class SecurityContextUtil {

    private SecurityContextUtil() {}

    public static JwtAuthenticatedPrincipal currentPrincipal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated()) {
            throw new IllegalStateException(
                    "No JWT principal in SecurityContext — is the endpoint protected?");
        }

        // JWT flow (Swagger / Postman / frontend with Bearer token)
        if (auth.getPrincipal() instanceof JwtAuthenticatedPrincipal principal) {
            return principal;
        }

        // OAuth2 session flow (browser Google login)
        if (auth.getPrincipal() instanceof CustomOAuth2User oAuth2User) {
            User user = oAuth2User.getResolvedUser();
            return new JwtAuthenticatedPrincipal(
                    user.getUserId(),
                    user.getEmail(),
                    user.getRole(),
                    user.getCollege() != null ? user.getCollege().getCollegeId() : null
            );
        }

        throw new IllegalStateException(
                "No JWT principal in SecurityContext — is the endpoint protected?");
    }

    public static UUID currentUserId() {
        return currentPrincipal().getUserId();
    }

    public static UUID currentCollegeId() {
        return currentPrincipal().getCollegeId();
    }

    public static void assertSameTenant(UUID requestedCollegeId) {
        UUID myCollegeId = currentCollegeId();
        if (myCollegeId == null) return;
        if (!myCollegeId.equals(requestedCollegeId)) {
            throw new TenantAccessDeniedException(
                    "You are not authorized to access resources of college: " + requestedCollegeId);
        }
    }
}