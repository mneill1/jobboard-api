package com.jobboard.api.service;

import com.jobboard.api.config.JwtUtil;
import com.jobboard.api.dto.AuthResponse;
import com.jobboard.api.dto.LoginRequest;
import com.jobboard.api.dto.RegisterRequest;
import com.jobboard.api.entity.User;
import com.jobboard.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthResponse register(RegisterRequest req) {
        if (userRepository.findByEmail(req.getEmail()).isPresent()) {
            throw new RuntimeException("Email already registered");
        }
        User user = new User();
        user.setEmail(req.getEmail());
        user.setPassword(passwordEncoder.encode(req.getPassword()));
        user.setRole(req.getRole());
        userRepository.save(user);
        return toResponse(user);
    }

    public AuthResponse login(LoginRequest req) {
        User user = userRepository.findByEmail(req.getEmail())
                .orElseThrow(() -> new RuntimeException("Invalid credentials"));
        if (!passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid credentials");
        }
        return toResponse(user);
    }

    private AuthResponse toResponse(User user) {
        AuthResponse response = new AuthResponse();
        response.setToken(jwtUtil.generateToken(user));
        response.setRole(user.getRole().name());
        return response;
    }
}
