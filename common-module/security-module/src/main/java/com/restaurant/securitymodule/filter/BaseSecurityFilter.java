package com.restaurant.securitymodule.filter;

import com.restaurant.securitymodule.config.SecurityProperties;
import com.restaurant.securitymodule.enums.SecurityType;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Base Security Filter - Manager Filter Chain
 * Routes requests to appropriate security filter based on SecurityType
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BaseSecurityFilter extends OncePerRequestFilter {

    private final SecurityProperties securityProperties;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

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

        String path = request.getRequestURI();
        SecurityType securityType = determineSecurityType(path);

        log.debug("Request path: {}, SecurityType: {}", path, securityType);

        // Mark that BaseSecurityFilter has processed this request
        // This prevents JwtAuthenticationFilter from running again if it's also in the
        // chain
        request.setAttribute(JwtAuthenticationFilter.ALREADY_FILTERED_ATTRIBUTE, Boolean.TRUE);

        switch (securityType) {
            case PUBLIC:
                // For PUBLIC endpoints, skip authentication and continue
                log.debug("PUBLIC endpoint, skipping authentication: {}", path);
                filterChain.doFilter(request, response);
                break;

            case JWT:
                // For JWT endpoints, delegate to JWT filter
                // Reset the attribute so JwtAuthenticationFilter can process
                request.removeAttribute(JwtAuthenticationFilter.ALREADY_FILTERED_ATTRIBUTE);
                log.debug("JWT endpoint, validating token: {}", path);
                jwtAuthenticationFilter.doFilter(request, response, filterChain);
                break;

            default:
                // Unknown security type, use default behavior
                log.warn("Unknown security type for path: {}, using default: {}",
                        path, securityProperties.getDefaultType());
                if (securityProperties.getDefaultType() == SecurityType.PUBLIC) {
                    filterChain.doFilter(request, response);
                } else {
                    request.removeAttribute(JwtAuthenticationFilter.ALREADY_FILTERED_ATTRIBUTE);
                    jwtAuthenticationFilter.doFilter(request, response, filterChain);
                }
        }
    }

    /**
     * Determine security type for the given path
     * Checks configured endpoint mappings, falls back to default
     */
    private SecurityType determineSecurityType(String path) {
        // Check configured endpoint mappings
        for (SecurityProperties.EndpointSecurity endpoint : securityProperties.getEndpoints()) {
            if (pathMatcher.match(endpoint.getPath(), path)) {
                return endpoint.getType();
            }
        }

        // Return default security type
        return securityProperties.getDefaultType();
    }
}
