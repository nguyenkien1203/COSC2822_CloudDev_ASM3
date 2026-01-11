package com.restaurant.profileservice.config;

import com.restaurant.securitymodule.filter.BaseSecurityFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Security configuration for Profile Service
 * Uses shared security-module for JWT authentication and CORS
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

        private final BaseSecurityFilter baseSecurityFilter;

        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
                http
                                // Enable CORS (uses CorsConfig from security-module)
                                .cors(org.springframework.security.config.Customizer.withDefaults())

                                // Disable CSRF since we're using stateless authentication
                                .csrf(AbstractHttpConfigurer::disable)

                                // Stateless session management
                                .sessionManagement(session -> session
                                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                                // Permit actuator endpoints for health checks (ALB target group)
                                .authorizeHttpRequests(auth -> auth
                                                .requestMatchers("/actuator/**", "/health").permitAll()
                                                .anyRequest().permitAll())

                                // Add base security filter from security-module
                                .addFilterBefore(baseSecurityFilter, UsernamePasswordAuthenticationFilter.class);

                return http.build();
        }
}
