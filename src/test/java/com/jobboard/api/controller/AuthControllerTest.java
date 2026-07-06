package com.jobboard.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobboard.api.TestSecurityConfig;
import com.jobboard.api.config.JwtUtil;
import com.jobboard.api.config.UserDetailsServiceImpl;
import com.jobboard.api.dto.AuthResponse;
import com.jobboard.api.dto.LoginRequest;
import com.jobboard.api.dto.RegisterRequest;
import com.jobboard.api.entity.UserRole;
import com.jobboard.api.repository.UserRepository;
import com.jobboard.api.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@Import(TestSecurityConfig.class)
class AuthControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private AuthService authService;
    @MockBean private JwtUtil jwtUtil;
    @MockBean private UserDetailsServiceImpl userDetailsService;
    @MockBean private UserRepository userRepository;

    // --- POST /api/auth/register ---

    @Test
    void register_validRequest_returns201WithAuthResponse() throws Exception {
        RegisterRequest req = new RegisterRequest();
        req.setEmail("alice@test.com");
        req.setPassword("password");
        req.setRole(UserRole.APPLICANT);

        AuthResponse authResponse = new AuthResponse();
        authResponse.setToken("jwt-token");
        authResponse.setRole("APPLICANT");

        when(authService.register(any(RegisterRequest.class))).thenReturn(authResponse);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").value("jwt-token"))
                .andExpect(jsonPath("$.role").value("APPLICANT"));
    }

    @Test
    void register_duplicateEmail_returns400() throws Exception {
        RegisterRequest req = new RegisterRequest();
        req.setEmail("duplicate@test.com");
        req.setPassword("password");
        req.setRole(UserRole.APPLICANT);

        when(authService.register(any(RegisterRequest.class)))
                .thenThrow(new RuntimeException("Email already registered"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    // --- POST /api/auth/login ---

    @Test
    void login_validCredentials_returns200WithToken() throws Exception {
        LoginRequest req = new LoginRequest();
        req.setEmail("alice@test.com");
        req.setPassword("password");

        AuthResponse authResponse = new AuthResponse();
        authResponse.setToken("jwt-token");
        authResponse.setRole("APPLICANT");

        when(authService.login(any(LoginRequest.class))).thenReturn(authResponse);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"));
    }

    @Test
    void login_invalidCredentials_returns400() throws Exception {
        LoginRequest req = new LoginRequest();
        req.setEmail("alice@test.com");
        req.setPassword("wrong");

        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new RuntimeException("Invalid credentials"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }
}
