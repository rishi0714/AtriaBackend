package com.campus.platform.security;

import com.campus.platform.common.enums.UserRole;
import com.campus.platform.common.util.SecurityContextUtil;
import com.campus.platform.exception.TenantAccessDeniedException;
import com.campus.platform.security.jwt.JwtAuthenticatedPrincipal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@DisplayName("SecurityContextUtil")
class SecurityContextUtilTest {

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private void setUpContext(UserRole role, UUID collegeId) {
        UUID userId = UUID.randomUUID();
        JwtAuthenticatedPrincipal principal =
                new JwtAuthenticatedPrincipal(userId, "test@college.edu", role, collegeId);
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(
                        principal, null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + role.name())));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    @DisplayName("currentPrincipal() returns correct principal from context")
    void currentPrincipal() {
        UUID cid = UUID.randomUUID();
        setUpContext(UserRole.STUDENT, cid);

        JwtAuthenticatedPrincipal p = SecurityContextUtil.currentPrincipal();
        assertThat(p.getRole()).isEqualTo(UserRole.STUDENT);
        assertThat(p.getCollegeId()).isEqualTo(cid);
    }

    @Test
    @DisplayName("assertSameTenant() passes when collegeIds match")
    void assertSameTenantPasses() {
        UUID cid = UUID.randomUUID();
        setUpContext(UserRole.CLUB_ADMIN, cid);

        assertThatNoException().isThrownBy(() -> SecurityContextUtil.assertSameTenant(cid));
    }

    @Test
    @DisplayName("assertSameTenant() throws when collegeIds differ")
    void assertSameTenantFails() {
        UUID myCollege    = UUID.randomUUID();
        UUID otherCollege = UUID.randomUUID();
        setUpContext(UserRole.CLUB_ADMIN, myCollege);

        assertThatThrownBy(() -> SecurityContextUtil.assertSameTenant(otherCollege))
                .isInstanceOf(TenantAccessDeniedException.class);
    }

    @Test
    @DisplayName("assertSameTenant() always passes for SUPER_ADMIN (null collegeId)")
    void superAdminBypassesTenantCheck() {
        setUpContext(UserRole.PLATFORM_OWNER, null); // null = no tenant restriction
        UUID anyCollege = UUID.randomUUID();

        assertThatNoException()
                .isThrownBy(() -> SecurityContextUtil.assertSameTenant(anyCollege));
    }
}
