package com.restaurant.securitymodule.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

/**
 * User principal containing authenticated user information
 * Extracted from JWT claims (Cognito sub) and stored in SecurityContext
 */
@Getter
@AllArgsConstructor
public class UserPrincipal {

    private final String userId;  // Cognito sub (UUID string)
    private final String email;
    private final Collection<? extends GrantedAuthority> authorities;
}
