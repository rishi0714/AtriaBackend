package com.campus.platform.security.oauth2;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;

/**
 * Invoked when OAuth2 login fails — most commonly because the user's email domain
 * is not registered as an active college tenant (access_denied).
 *
 * Returns RFC 7807-style JSON instead of Spring's default redirect to /login?error.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception) throws IOException {

        log.warn("OAuth2 authentication failure: {}", exception.getMessage());

        int status = HttpServletResponse.SC_FORBIDDEN; // 403 default for domain rejection
        String errorCode = "access_denied";
        String detail = exception.getMessage();

        if (exception instanceof OAuth2AuthenticationException oae) {
            errorCode = oae.getError().getErrorCode();
            // invalid_token → 401, access_denied → 403
            if ("invalid_token".equals(errorCode)) {
                status = HttpServletResponse.SC_UNAUTHORIZED;
            }
        }

        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        objectMapper.writeValue(response.getWriter(), Map.of(
                "type", "https://campus-platform.example.com/errors/" + errorCode,
                "title", "Authentication Failed",
                "status", status,
                "detail", detail != null ? detail : "OAuth2 login failed.",
                "timestamp", Instant.now().toString()
        ));
    }
}
