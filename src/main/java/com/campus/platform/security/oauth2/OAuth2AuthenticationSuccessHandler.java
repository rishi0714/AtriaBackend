package com.campus.platform.security.oauth2;

import com.campus.platform.security.jwt.JwtTokenProvider;
import com.campus.platform.user.entity.User;
import com.campus.platform.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Invoked by Spring Security after a successful OAuth2 login.
 *
 * Behaviour:
 *  - Generates a platform JWT (RS256) embedding userId, email, role, collegeId.
 *  - If a frontend redirect URI is configured, appends the token as a query param
 *    and redirects (SPA flow). Otherwise, returns the token as a JSON body
 *    (useful during local development / Postman testing).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtTokenProvider jwtTokenProvider;
    private final ObjectMapper objectMapper;
    private final UserRepository userRepository;

    @Value("${app.oauth2.redirect-uri:}")
    private String frontendRedirectUri;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        CustomOAuth2User oAuth2User = (CustomOAuth2User) authentication.getPrincipal();
        User user = oAuth2User.getResolvedUser();

        String accessToken = jwtTokenProvider.generateToken(
                user.getUserId(),
                user.getEmail(),
                user.getRole(),
                user.getCollege() != null ? user.getCollege().getCollegeId() : null
        );

        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getUserId());
        user.setRefreshToken(refreshToken);
        userRepository.save(user);

        // ← removed: Set-Cookie header entirely

        log.info("JWT issued for userId: {}, role: {}", user.getUserId(), user.getRole());

        if (frontendRedirectUri != null && !frontendRedirectUri.isBlank()) {
            // ← both tokens in redirect URL
            String redirectUrl = frontendRedirectUri
                    + "?token=" + accessToken
                    + "&refreshToken=" + refreshToken;
            getRedirectStrategy().sendRedirect(request, response, redirectUrl);
        } else {
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setStatus(HttpServletResponse.SC_OK);

            Map<String, Object> responseBody = new HashMap<>();
            responseBody.put("token", accessToken);
            responseBody.put("refreshToken", refreshToken);  // ← add this
            responseBody.put("userId", user.getUserId().toString());
            responseBody.put("email", user.getEmail());
            responseBody.put("role", user.getRole().name());
            if (user.getCollege() != null) {
                responseBody.put("collegeId", user.getCollege().getCollegeId().toString());
            }

            objectMapper.writeValue(response.getWriter(), responseBody);
        }
    }
}
