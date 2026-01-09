package com.restaurant.securitymodule.filter;

import com.restaurant.securitymodule.config.SecurityProperties;
import com.restaurant.securitymodule.model.UserPrincipal;
import com.restaurant.securitymodule.service.CognitoJwtValidator;
import com.restaurant.securitymodule.service.CognitoJwtValidator.CognitoClaims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * JWT Authentication Filter for Cognito tokens
 * Extracts and validates Cognito JWT from cookie, sets SecurityContext
 * 
 * This filter is invoked by BaseSecurityFilter for JWT-protected endpoints.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final CognitoJwtValidator cognitoJwtValidator;
    private final SecurityProperties securityProperties;
    private final org.springframework.util.AntPathMatcher pathMatcher = new org.springframework.util.AntPathMatcher();

    // Request attribute to indicate this filter was already invoked by
    // BaseSecurityFilter
    public static final String ALREADY_FILTERED_ATTRIBUTE = "JWT_FILTER_ALREADY_APPLIED";

    /**
     * Skip this filter entirely for actuator endpoints (health checks)
     * This ensures ALB health checks always pass regardless of security config
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return pathMatcher.match("/actuator/**", path);
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        // Check if already processed by BaseSecurityFilter dispatch
        if (Boolean.TRUE.equals(request.getAttribute(ALREADY_FILTERED_ATTRIBUTE))) {
            filterChain.doFilter(request, response);
            return;
        }

        // Mark as processed
        request.setAttribute(ALREADY_FILTERED_ATTRIBUTE, Boolean.TRUE);

        // Check if validator is initialized
        if (!cognitoJwtValidator.isInitialized()) {
            log.warn("Cognito JWT validator not initialized, skipping JWT authentication");
            sendUnauthorizedError(response, "Authentication service not available");
            return;
        }

        try {
            // Extract JWT from cookie
            String token = extractTokenFromCookie(request);

            if (token == null || token.isEmpty()) {
                log.debug("No JWT token found in cookie for path: {}", request.getRequestURI());
                sendUnauthorizedError(response, "Authentication required");
                return;
            }

            // Validate and parse Cognito JWT
            CognitoClaims claims = cognitoJwtValidator.validateToken(token);

            // Extract user information
            String email = claims.getEmail();
            String sub = claims.getSub();
            List<String> roles = claims.getRoles();

            log.debug("Cognito JWT validated for user: {}, roles: {}", email, roles);

            // Create Spring Security authorities from Cognito groups
            List<SimpleGrantedAuthority> authorities = roles.stream()
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList());

            // Create principal with Cognito sub as userId
            UserPrincipal principal = new UserPrincipal(
                    sub, // Use Cognito sub as userId
                    email,
                    authorities);

            // Create authentication token
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    principal,
                    null,
                    authorities);

            // Set in SecurityContext
            SecurityContextHolder.getContext().setAuthentication(authentication);

            log.debug("JWT authentication successful for user: {} on path: {}",
                    email, request.getRequestURI());

            // Continue filter chain
            filterChain.doFilter(request, response);

        } catch (Exception e) {
            log.warn("JWT validation failed: {}", e.getMessage());
            sendUnauthorizedError(response, "Invalid or expired token");
        }
    }

    /**
     * Extract JWT token from cookie
     */
    private String extractTokenFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }

        String cookieName = securityProperties.getJwt().getCookieName();
        return Arrays.stream(cookies)
                .filter(cookie -> cookieName.equals(cookie.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }

    /**
     * Send 401 Unauthorized error response
     */
    private void sendUnauthorizedError(HttpServletResponse response, String message)
            throws IOException {
        response.setContentType("application/json");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.getWriter().write(
                String.format("{\"error\":\"Unauthorized\",\"message\":\"%s\"}", message));
    }
}
