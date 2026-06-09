package com.campus.platform.security.jwt;

import com.campus.platform.common.enums.UserRole;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

/**
 * Handles RS256 JWT creation and validation.
 *
 * Keys are injected as Base64-encoded PEM bodies (without headers/footers)
 * via application properties. Generate a key pair with:
 *
 *   openssl genrsa -out private.pem 2048
 *   openssl rsa -in private.pem -pubout -out public.pem
 *
 * Then strip the header/footer lines and base64-encode for the property values.
 */
@Slf4j
@Component
public class JwtTokenProvider {

    @Value("${app.jwt.private-key}")
    private String privateKeyBase64;

    @Value("${app.jwt.public-key}")
    private String publicKeyBase64;

    @Value("${app.jwt.expiration-ms:86400000}") // 24 hours default
    private long expirationMs;

    private PrivateKey privateKey;
    private PublicKey publicKey;

    @PostConstruct
    public void init() throws Exception {
        KeyFactory kf = KeyFactory.getInstance("RSA");

        byte[] privBytes = Base64.getDecoder()
                .decode(privateKeyBase64.replaceAll("\\s", ""));

        privateKey = kf.generatePrivate(
                new PKCS8EncodedKeySpec(privBytes)
        );

        byte[] pubBytes = Base64.getDecoder()
                .decode(publicKeyBase64.replaceAll("\\s", ""));

        publicKey = kf.generatePublic(
                new X509EncodedKeySpec(pubBytes)
        );
    }

    // ── Token creation ───────────────────────────────────────────────────────────

    public String generateToken(UUID userId, String email, UserRole role, UUID collegeId) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .subject(userId.toString())
                .claim("email", email)
                .claim("role", role.name())
                .claim("collegeId", collegeId != null ? collegeId.toString() : null)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(privateKey, Jwts.SIG.RS256)
                .compact();
    }

    // ── Token parsing ────────────────────────────────────────────────────────────

    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(publicKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("JWT expired: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.warn("JWT unsupported: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            log.warn("JWT malformed: {}", e.getMessage());
        } catch (SecurityException e) {
            log.warn("JWT signature invalid: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.warn("JWT claims empty: {}", e.getMessage());
        }
        return false;
    }

    // ── Claim extractors ─────────────────────────────────────────────────────────

    public UUID extractUserId(String token) {
        return UUID.fromString(parseToken(token).getSubject());
    }

    public String extractEmail(String token) {
        return parseToken(token).get("email", String.class);
    }

    public UserRole extractRole(String token) {
        return UserRole.valueOf(parseToken(token).get("role", String.class));
    }

    public UUID extractCollegeId(String token) {
        String raw = parseToken(token).get("collegeId", String.class);
        return raw != null ? UUID.fromString(raw) : null;
    }
}
