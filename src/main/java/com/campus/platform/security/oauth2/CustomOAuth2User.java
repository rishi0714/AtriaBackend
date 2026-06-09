package com.campus.platform.security.oauth2;

import com.campus.platform.user.entity.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@Getter
public class CustomOAuth2User implements OidcUser {

    private final OAuth2User delegate;
    private final User resolvedUser;
    private final OidcIdToken idToken;
    private final OidcUserInfo userInfo;

    /** Used by CustomOAuth2UserService (non-OIDC, or OIDC via OidcUser delegate) */
    public CustomOAuth2User(OAuth2User delegate, User resolvedUser) {
        this.delegate = delegate;
        this.resolvedUser = resolvedUser;
        // If the delegate is already an OidcUser (Google flow), extract tokens
        if (delegate instanceof OidcUser oidc) {
            this.idToken = oidc.getIdToken();
            this.userInfo = oidc.getUserInfo();
        } else {
            this.idToken = null;
            this.userInfo = null;
        }
    }

    // --- OAuth2User ---

    @Override
    public Map<String, Object> getAttributes() {
        return delegate.getAttributes();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + resolvedUser.getRole().name()));
    }

    @Override
    public String getName() {
        return resolvedUser.getEmail();
    }

    // --- OidcUser ---

    @Override
    public Map<String, Object> getClaims() {
        return idToken != null ? idToken.getClaims() : Map.of();
    }

    @Override
    public OidcUserInfo getUserInfo() {
        return userInfo;
    }

    @Override
    public OidcIdToken getIdToken() {
        return idToken;
    }
}