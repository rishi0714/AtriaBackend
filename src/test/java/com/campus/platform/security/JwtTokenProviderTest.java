package com.campus.platform.security;

import com.campus.platform.common.enums.UserRole;
import com.campus.platform.security.jwt.JwtTokenProvider;
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

        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        KeyPair kp = kpg.generateKeyPair();

        String privateB64 =
                Base64.getEncoder()
                        .encodeToString(kp.getPrivate().getEncoded());

        String publicB64 =
                Base64.getEncoder()
                        .encodeToString(kp.getPublic().getEncoded());

        provider = new JwtTokenProvider();

        setField(provider, "privateKeyBase64", privateB64);
        setField(provider, "publicKeyBase64", publicB64);
        setField(provider, "expirationMs", 86_400_000L);

        provider.init();

        userId = UUID.randomUUID();
        collegeId = UUID.randomUUID();
    }

    @Test
    @DisplayName("generated token validates successfully")
    void tokenValidates() {

        String token = provider.generateToken(
                userId,
                "student@test.edu",
                UserRole.STUDENT,
                collegeId
        );

        assertThat(provider.validateToken(token))
                .isTrue();
    }

    @Test
    @DisplayName("claims round-trip: userId, email, role, collegeId")
    void claimsRoundTrip() {

        String token = provider.generateToken(
                userId,
                "clubadmin@test.edu",
                UserRole.CLUB_ADMIN,
                collegeId
        );

        assertThat(provider.extractUserId(token))
                .isEqualTo(userId);

        assertThat(provider.extractEmail(token))
                .isEqualTo("clubadmin@test.edu");

        assertThat(provider.extractRole(token))
                .isEqualTo(UserRole.CLUB_ADMIN);

        assertThat(provider.extractCollegeId(token))
                .isEqualTo(collegeId);
    }

    @Test
    @DisplayName("null collegeId is preserved for PLATFORM_OWNER")
    void platformOwnerNullCollegeId() {

        String token = provider.generateToken(
                userId,
                "owner@platform.com",
                UserRole.PLATFORM_OWNER,
                null
        );

        assertThat(provider.extractCollegeId(token))
                .isNull();
    }

    @Test
    @DisplayName("extractRole returns PLATFORM_OWNER")
    void extractPlatformOwnerRole() {

        String token = provider.generateToken(
                userId,
                "owner@platform.com",
                UserRole.PLATFORM_OWNER,
                null
        );

        assertThat(provider.extractRole(token))
                .isEqualTo(UserRole.PLATFORM_OWNER);
    }

    @Test
    @DisplayName("tampered token fails validation")
    void tamperedToken() {

        String token = provider.generateToken(
                userId,
                "student@test.edu",
                UserRole.STUDENT,
                collegeId
        );

        String tampered =
                token.substring(0, token.length() - 5) + "XXXXX";

        assertThat(provider.validateToken(tampered))
                .isFalse();
    }

    @Test
    @DisplayName("blank string fails validation without throwing")
    void blankTokenReturnsFalse() {

        assertThat(provider.validateToken(""))
                .isFalse();

        assertThat(provider.validateToken("not.a.jwt"))
                .isFalse();
    }

    private void setField(
            Object target,
            String fieldName,
            Object value
    ) throws Exception {

        var field =
                target.getClass().getDeclaredField(fieldName);

        field.setAccessible(true);
        field.set(target, value);
    }
}