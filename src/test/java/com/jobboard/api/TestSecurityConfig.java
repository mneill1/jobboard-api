package com.jobboard.api;

import com.jobboard.api.config.JwtAuthFilter;
import com.jobboard.api.config.JwtUtil;
import com.jobboard.api.config.UserDetailsServiceImpl;
import com.jobboard.api.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import java.io.IOException;

import static org.mockito.Mockito.mock;

/**
 * Provides no-op security infrastructure for @WebMvcTest slices.
 *
 * - testSecurityFilterChain: permits all requests (no auth required in tests)
 * - noOpJwtAuthFilter: passes every request through without JWT validation
 * - passwordEncoder: mock bean to satisfy SecurityConfig dependency
 *
 * Usage: @Import(TestSecurityConfig.class) on @WebMvcTest controller test classes.
 * Each test class must also @MockBean: JwtUtil, UserDetailsServiceImpl.
 */
@TestConfiguration
public class TestSecurityConfig {

    @Bean
    @Order(1)
    public SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }

    /**
     * Replace the real JwtAuthFilter with a pass-through that never blocks requests.
     * The real @MockBean of JwtAuthFilter swallows requests (doFilter not called by mock).
     */
    @Bean
    public JwtAuthFilter noOpJwtAuthFilter(JwtUtil jwtUtil, UserRepository userRepository) {
        return new JwtAuthFilter(jwtUtil, userRepository) {
            @Override
            protected void doFilterInternal(HttpServletRequest request,
                                            HttpServletResponse response,
                                            FilterChain filterChain) throws ServletException, IOException {
                filterChain.doFilter(request, response);
            }
        };
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return mock(BCryptPasswordEncoder.class);
    }
}
