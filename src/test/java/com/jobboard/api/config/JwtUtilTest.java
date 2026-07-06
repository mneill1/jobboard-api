package com.jobboard.api.config;

import com.jobboard.api.entity.User;
import com.jobboard.api.entity.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class JwtUtilTest {

    // Must be at least 256 bits (32 chars) for HS256
    private static final String SECRET =
            "test-secret-key-must-be-at-least-256-bits-long-for-hs256-okay";

    private JwtUtil jwtUtil;
    private User user;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil(SECRET);

        user = new User();
        user.setId(1L);
        user.setEmail("alice@test.com");
        user.setRole(UserRole.APPLICANT);
        user.setPassword("hashed");
    }

    @Test
    void generateToken_producesNonBlankToken() {
        String token = jwtUtil.generateToken(user);

        assertThat(token).isNotBlank();
    }

    @Test
    void extractEmail_returnsSubjectFromToken() {
        String token = jwtUtil.generateToken(user);

        assertThat(jwtUtil.extractEmail(token)).isEqualTo("alice@test.com");
    }

    @Test
    void isTokenValid_validToken_returnsTrue() {
        String token = jwtUtil.generateToken(user);

        assertThat(jwtUtil.isTokenValid(token, user)).isTrue();
    }

    @Test
    void isTokenValid_wrongUser_returnsFalse() {
        String token = jwtUtil.generateToken(user);

        User otherUser = new User();
        otherUser.setEmail("other@test.com");
        otherUser.setPassword("hashed");
        otherUser.setRole(UserRole.APPLICANT);

        assertThat(jwtUtil.isTokenValid(token, otherUser)).isFalse();
    }

    @Test
    void isTokenValid_tamperedToken_throwsException() {
        String token = jwtUtil.generateToken(user);
        String tampered = token.substring(0, token.length() - 4) + "XXXX";

        assertThatThrownBy(() -> jwtUtil.isTokenValid(tampered, user));
    }
}
