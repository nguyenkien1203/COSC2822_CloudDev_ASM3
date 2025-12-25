package com.restaurant.authservice.filter;

import com.restaurant.authservice.service.CognitoJwtValidator;
import com.restaurant.authservice.service.CognitoJwtValidator.CognitoClaims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Cookie;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * JWT Cookie Filter for Cognito tokens
 * Extracts and validates Cognito JWT from cookies
 */
@Slf4j
@Component
public class JwtCookieFilter extends OncePerRequestFilter {

    @Value("${jwt.cookie-name:auth_token}")
    private String cookieName;

    @Autowired
    private CognitoJwtValidator cognitoJwtValidator;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        // 1. Find the access token cookie (Cognito ID token for user info)
        String token = null;
        if (request.getCookies() != null) {
            log.debug("Total cookies received: {}", request.getCookies().length);

            token = Arrays.stream(request.getCookies())
                    .filter(c -> cookieName.equals(c.getName()))
                    .map(Cookie::getValue)
                    .findFirst()
                    .orElse(null);

            if (token != null) {
                log.debug("Found '{}' cookie with token length: {}", cookieName, token.length());
            } else {
                log.debug("No '{}' cookie found", cookieName);
            }
        }

        // 2. If token exists and no one is logged in yet
        if (token != null && !token.isEmpty() && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                // 3. Validate and parse Cognito JWT
                CognitoClaims claims = cognitoJwtValidator.validateToken(token);

                // 4. Extract user info
                String username = claims.getEmail();
                List<String> roles = claims.getRoles();

                log.debug("Cognito JWT validated for user: {}, roles: {}", username, roles);

                // 5. Create Spring Security authorities from Cognito groups
                List<SimpleGrantedAuthority> authorities = roles.stream()
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList());

                // 6. Create authentication token
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        username,
                        null,
                        authorities);

                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // 7. Set SecurityContext
                SecurityContextHolder.getContext().setAuthentication(authToken);

                log.debug("Successfully authenticated Cognito user: {} with authorities: {}", username, authorities);

            } catch (Exception e) {
                // Token invalid or expired
                log.warn("Cognito JWT validation failed: {}", e.getMessage());
                // Don't set authentication - let the request continue (might be a public
                // endpoint)
            }
        }

        filterChain.doFilter(request, response);
    }
}
