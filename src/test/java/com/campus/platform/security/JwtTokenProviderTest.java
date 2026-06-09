package com.campus.platform.security;

import com.campus.platform.common.enums.UserRole;
import com.campus.platform.security.jwt.JwtTokenProvider;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("JwtTokenProvider")
class JwtTokenProviderTest {

    private JwtTokenProvider provider;

    private UUID userId;
    private UUID collegeId;

    @BeforeEach
    void setUp() throws Exception {
        // Generate a fresh RSA key pair for each test — no file I/O needed
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        KeyPair kp = kpg.generateKeyPair();

        String privateB64 = Base64.getEncoder().encodeToString(kp.getPrivate().getEncoded());
        String publicB64  = Base64.getEncoder().encodeToString(kp.getPublic().getEncoded());

        provider = new JwtTokenProvider();

        // Inject via reflection (field names match @Value fields in provider)
        setField(provider, "privateKeyBase64", privateB64);
        setField(provider, "publicKeyBase64",  publicB64);
        setField(provider, "expirationMs",     86_400_000L);

        provider.init();

        userId    = UUID.randomUUID();
        collegeId = UUID.randomUUID();
    }

    @Test
    @DisplayName("generated token validates successfully")
    void tokenValidates() {
        String token = provider.generateToken(userId, "s@test.edu", UserRole.STUDENT, collegeId);
        assertThat(provider.validateToken(token)).isTrue();
    }

    @Test
    @DisplayName("claims round-trip: userId, email, role, collegeId")
    void claimsRoundTrip() {
        String token = provider.generateToken(userId, "s@test.edu", UserRole.CLUB_ADMIN, collegeId);

        assertThat(provider.extractUserId(token)).isEqualTo(userId);
        assertThat(provider.extractEmail(token)).isEqualTo("s@test.edu");
        assertThat(provider.extractRole(token)).isEqualTo(UserRole.CLUB_ADMIN);
        assertThat(provider.extractCollegeId(token)).isEqualTo(collegeId);
    }

    @Test
    @DisplayName("null collegeId is preserved for SUPER_ADMIN")
    void superAdminNullCollegeId() {
        String token = provider.generateToken(userId, "sa@platform.com", UserRole.SUPER_ADMIN, null);
        assertThat(provider.extractCollegeId(token)).isNull();
    }

    @Test
    @DisplayName("tampered token fails validation")
    void tamperedToken() {
        String token = provider.generateToken(userId, "s@test.edu", UserRole.STUDENT, collegeId);
        String tampered = token.substring(0, token.length() - 5) + "XXXXX";
        assertThat(provider.validateToken(tampered)).isFalse();
    }

    @Test
    @DisplayName("blank string fails validation without throwing")
    void blankTokenReturnsFalse() {
        assertThat(provider.validateToken("")).isFalse();
        assertThat(provider.validateToken("not.a.jwt")).isFalse();
    }

    // ── Reflection helper ────────────────────────────────────────────────────────

    private void setField(Object target, String fieldName, Object value) throws Exception {
        var field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
