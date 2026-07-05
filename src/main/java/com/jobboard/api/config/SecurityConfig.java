package com.jobboard.api.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.config.Customizer;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final UserDetailsServiceImpl userDetailsService;

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(Customizer.withDefaults())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Public read endpoints
                .requestMatchers(HttpMethod.GET, "/api/jobs/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/jobs/search").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/companies/**").permitAll()
                // Auth endpoints
                .requestMatchers("/api/auth/**").permitAll()
                // SOAP, Swagger, Actuator
                .requestMatchers("/ws/**").permitAll()
                .requestMatchers("/swagger-ui/**", "/swagger-ui.html").permitAll()
                .requestMatchers("/v3/api-docs/**").permitAll()
                .requestMatchers("/actuator/**").permitAll()
                // COMPANY-only mutations
                .requestMatchers(HttpMethod.POST, "/api/jobs").hasRole("COMPANY")
                .requestMatchers(HttpMethod.PUT, "/api/jobs/**").hasRole("COMPANY")
                .requestMatchers(HttpMethod.DELETE, "/api/jobs/**").hasRole("COMPANY")
                .requestMatchers(HttpMethod.GET, "/api/jobs/*/applications").hasRole("COMPANY")
                .requestMatchers(HttpMethod.POST, "/api/companies").hasRole("COMPANY")
                .requestMatchers(HttpMethod.PUT, "/api/companies/**").hasRole("COMPANY")
                .requestMatchers(HttpMethod.DELETE, "/api/companies/**").hasRole("COMPANY")
                // Apply — any authenticated user
                .requestMatchers(HttpMethod.POST, "/api/jobs/*/apply").authenticated()
                // Everything else requires authentication
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
