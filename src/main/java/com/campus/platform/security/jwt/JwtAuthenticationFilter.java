package com.campus.platform.security.jwt;

import com.campus.platform.common.enums.UserRole;
import com.campus.platform.user.entity.User;
import com.campus.platform.user.repository.UserRepository;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String token = extractBearerToken(request);

        if (StringUtils.hasText(token) && jwtTokenProvider.validateToken(token)) {
            boolean blocked = processToken(token, response);
            if (blocked) return;
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Parses the token, runs role-based checks, and sets the security context.
     *
     * @return true if the request was blocked (response already written), false otherwise.
     */
    private boolean processToken(String token, HttpServletResponse response) throws IOException {
        try {
            Claims claims = jwtTokenProvider.parseToken(token);

            UUID userId     = UUID.fromString(claims.getSubject());
            String email    = claims.get("email", String.class);
            UserRole role   = UserRole.valueOf(claims.get("role", String.class));
            String rawCollegeId = claims.get("collegeId", String.class);
            UUID collegeId  = rawCollegeId != null ? UUID.fromString(rawCollegeId) : null;

            if (requiresCollegeCheck(role)) {
                boolean blocked = runCollegeActiveCheck(userId, role, response);
                if (blocked) return true;
            }

            setAuthentication(userId, email, role, collegeId);

        } catch (Exception e) {
            log.warn("Could not set user authentication from JWT: {}", e.getMessage());
            SecurityContextHolder.clearContext();
        }

        return false;
    }

    /**
     * Returns true if the role requires a real-time college active check.
     */
    private boolean requiresCollegeCheck(UserRole role) {
        return role == UserRole.COLLEGE_ADMIN || role == UserRole.CLUB_ADMIN;
    }

    /**
     * Checks that the user exists and their college is active.
     *
     * @return true if the request should be blocked, false if it can proceed.
     */
    private boolean runCollegeActiveCheck(UUID userId,
                                          UserRole role,
                                          HttpServletResponse response) throws IOException {
        User user = userRepository.findByIdWithCollege(userId).orElse(null);

        if (user == null) {
            log.warn("{} JWT valid but user not found: {}", role, userId);
            SecurityContextHolder.clearContext();
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "User not found.");
            return true;
        }

        if (isCollegeInactive(user)) {
            log.warn("{} {} blocked — college is inactive.", role, userId);
            SecurityContextHolder.clearContext();
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Your college has been deactivated.");
            return true;
        }

        return false;
    }

    /**
     * Returns true if the user has a college that is currently inactive.
     */
    private boolean isCollegeInactive(User user) {
        return user.getCollege() != null && !user.getCollege().isActive();
    }

    /**
     * Builds the authentication object and sets it in the security context.
     */
    private void setAuthentication(UUID userId, String email, UserRole role, UUID collegeId) {
        JwtAuthenticatedPrincipal principal =
                new JwtAuthenticatedPrincipal(userId, email, role, collegeId);

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        principal,
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + role.name()))
                );

        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private String extractBearerToken(HttpServletRequest request) {
        String header = request.getHeader(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(header) && header.startsWith(BEARER_PREFIX)) {
            return header.substring(BEARER_PREFIX.length());
        }
        return null;
    }
}