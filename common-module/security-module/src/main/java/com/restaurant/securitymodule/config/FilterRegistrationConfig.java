package com.restaurant.securitymodule.config;

import com.restaurant.securitymodule.filter.BaseSecurityFilter;
import com.restaurant.securitymodule.filter.JwtAuthenticationFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration to prevent security filters from being auto-registered
 * as servlet filters. They should only run within the Spring Security chain.
 */
@Configuration
public class FilterRegistrationConfig {

    /**
     * Disable auto-registration of BaseSecurityFilter as a servlet filter.
     * It should only be invoked via Spring Security's filter chain.
     */
    @Bean
    public FilterRegistrationBean<BaseSecurityFilter> baseSecurityFilterRegistration(
            BaseSecurityFilter filter) {
        FilterRegistrationBean<BaseSecurityFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    /**
     * Disable auto-registration of JwtAuthenticationFilter as a servlet filter.
     * It should only be invoked via BaseSecurityFilter dispatch.
     */
    @Bean
    public FilterRegistrationBean<JwtAuthenticationFilter> jwtAuthFilterRegistration(
            JwtAuthenticationFilter filter) {
        FilterRegistrationBean<JwtAuthenticationFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }
}
