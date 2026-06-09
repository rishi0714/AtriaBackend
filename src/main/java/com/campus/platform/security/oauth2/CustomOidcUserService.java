package com.campus.platform.security.oauth2;

import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomOidcUserService extends OidcUserService {

    private final CustomOAuth2UserService customOAuth2UserService;

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        // Let Spring fetch the OIDC user info (populates attributes like email, sub, etc.)
        super.loadUser(userRequest);

        // OidcUserRequest extends OAuth2UserRequest — pass it directly.
        // customOAuth2UserService.loadUser() calls super.loadUser() again internally,
        // which is a cheap extra call but keeps zero changes to your existing service.
        return (OidcUser) customOAuth2UserService.loadUser(userRequest);
    }
}