package com.campus.platform.security.oauth2;

import com.campus.platform.college.entity.College;
import com.campus.platform.college.service.CollegeService;
import com.campus.platform.user.entity.User;
import com.campus.platform.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final CollegeService collegeService;
    private final UserService userService;

    @Value("${app.platform-owner-email}")
    private String platformOwnerEmail;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest)
            throws OAuth2AuthenticationException {

        OAuth2User oAuth2User = super.loadUser(userRequest);

        String email      = oAuth2User.getAttribute("email");
        String googleSub  = oAuth2User.getAttribute("sub");
        String fullName   = oAuth2User.getAttribute("name");
        String pictureUrl = oAuth2User.getAttribute("picture");

        if (email == null || googleSub == null) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("invalid_token"),
                    "Google profile missing required fields (email or sub)."
            );
        }

        email = email.toLowerCase();

        log.info("OAuth2 login attempt — email: {}", email);

        // -------------------------------------------------------
        // PLATFORM_OWNER BYPASS (no college domain required)
        // -------------------------------------------------------
        if (email.equalsIgnoreCase(platformOwnerEmail)) {
            User owner = userService.provisionPlatformOwner(
                    googleSub, email, fullName, pictureUrl);
            log.info("PLATFORM_OWNER login successful: {}", email);
            return new CustomOAuth2User(oAuth2User, owner);
        }

        // -------------------------------------------------------
        // COLLEGE DOMAIN FLOW — unknown domains become guests
        // -------------------------------------------------------
        String domain = extractDomain(email);

        log.info("Resolving college for domain: {}", domain);

        College college = null;
        try {
            college = collegeService.resolveByDomain(domain);
            log.info("College resolved: {} for domain: {}", college.getName(), domain);
        } catch (ResponseStatusException ex) {
            // Unknown domain — allowed, provisions as guest student (college = null)
            log.info("No college found for domain: {} — provisioning as guest student", domain);
        }

        User user = userService.provisionUser(
                googleSub, email, fullName, pictureUrl, college);

        log.info("OAuth2 login success — userId: {}, role: {}, college: {}",
                user.getUserId(),
                user.getRole(),
                college != null ? college.getName() : "guest (no college)");

        return new CustomOAuth2User(oAuth2User, user);
    }

    private String extractDomain(String email) {
        int atIndex = email.indexOf('@');
        if (atIndex < 0 || atIndex == email.length() - 1) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("invalid_token"),
                    "Email address has no valid domain: " + email
            );
        }
        return email.substring(atIndex + 1).toLowerCase();
    }
}