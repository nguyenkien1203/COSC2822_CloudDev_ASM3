package com.restaurant.authservice.config;

import com.restaurant.authservice.filter.JwtCookieFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Security configuration for JWT-based authentication with role-based
 * authorization
 * Supports both cookie-based (web app) and header-based (service-to-service)
 * authentication
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true, securedEnabled = true)
public class SecurityConfig {

    @Autowired
    private JwtCookieFilter jwtCookieFilter; // Inject your new filter

    /**
     * Password encoder bean for hashing and verifying passwords
     * Uses BCrypt with strength 10 (default)
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * CORS configuration for cross-origin requests
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList(
                "http://localhost:3000",
                "http://localhost:5173",
                "http://restaurant-alb-696207792.us-east-1.elb.amazonaws.com",
                "https://nvkncf9aa6.execute-api.us-east-1.amazonaws.com"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource())) // Enable CORS with custom config
                .csrf(csrf -> csrf.disable()) // Disable CSRF for stateless JWT authentication
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints - no authentication required
                        .requestMatchers("/api/auth/login", "/api/auth/register").permitAll()
                        .requestMatchers("/api/auth/logout", "/api/auth/refresh").permitAll()
                        .requestMatchers("/api/auth/confirm", "/api/auth/resend-code").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()

                        // User endpoints - requires authentication
                        .requestMatchers("/api/auth/me").authenticated()

                        // Admin-only endpoints
                        .requestMatchers("/api/auth/admin/**").hasRole("ADMIN")

                        // Manager and Admin endpoints
                        .requestMatchers("/api/auth/manager/**").hasAnyRole("MANAGER", "ADMIN")

                        // All other requests require authentication
                        .anyRequest().authenticated())
                // Configure exception handling
                .exceptionHandling(ex -> ex
                        // Handle authentication failures (401)
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setContentType("application/json");
                            response.setStatus(401);
                            response.getWriter()
                                    .write("{\"error\":\"Unauthorized\",\"message\":\"Authentication required\"}");
                        })
                        // Handle authorization failures (403)
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setContentType("application/json");
                            response.setStatus(403);
                            response.getWriter().write("{\"error\":\"Forbidden\",\"message\":\"Access denied\"}");
                        }))
                // Add the JWT filter before UsernamePasswordAuthenticationFilter
                .addFilterBefore(jwtCookieFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}