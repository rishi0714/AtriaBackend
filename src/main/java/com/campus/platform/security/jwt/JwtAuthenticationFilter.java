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
            try {
                Claims claims = jwtTokenProvider.parseToken(token);

                UUID userId = UUID.fromString(claims.getSubject());
                String email = claims.get("email", String.class);
                UserRole role = UserRole.valueOf(claims.get("role", String.class));
                String rawCollegeId = claims.get("collegeId", String.class);
                UUID collegeId = rawCollegeId != null ? UUID.fromString(rawCollegeId) : null;

                // Real-time college active check for COLLEGE_ADMIN
                // Real-time college active check for COLLEGE_ADMIN and CLUB_ADMIN
                if (role == UserRole.COLLEGE_ADMIN || role == UserRole.CLUB_ADMIN) {
                    User user = userRepository.findByIdWithCollege(userId).orElse(null);
                    if (user == null) {
                        log.warn("{} JWT valid but user not found: {}", role, userId);
                        SecurityContextHolder.clearContext();
                        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "User not found.");
                        return;
                    }
                    if (user.getCollege() != null && !user.getCollege().isActive()) {
                        log.warn("{} {} blocked — college is inactive.", role, userId);
                        SecurityContextHolder.clearContext();
                        response.sendError(HttpServletResponse.SC_FORBIDDEN, "Your college has been deactivated.");
                        return;
                    }
                }

                JwtAuthenticatedPrincipal principal =
                        new JwtAuthenticatedPrincipal(userId, email, role, collegeId);

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                principal,
                                null,
                                List.of(new SimpleGrantedAuthority("ROLE_" + role.name()))
                        );

                SecurityContextHolder.getContext().setAuthentication(authentication);

            } catch (Exception e) {
                log.warn("Could not set user authentication from JWT: {}", e.getMessage());
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }

    private String extractBearerToken(HttpServletRequest request) {
        String header = request.getHeader(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(header) && header.startsWith(BEARER_PREFIX)) {
            return header.substring(BEARER_PREFIX.length());
        }
        return null;
    }
}