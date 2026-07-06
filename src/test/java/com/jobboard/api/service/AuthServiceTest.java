package com.jobboard.api.service;

import com.jobboard.api.config.JwtUtil;
import com.jobboard.api.dto.AuthResponse;
import com.jobboard.api.dto.LoginRequest;
import com.jobboard.api.dto.RegisterRequest;
import com.jobboard.api.entity.Company;
import com.jobboard.api.entity.User;
import com.jobboard.api.entity.UserRole;
import com.jobboard.api.repository.CompanyRepository;
import com.jobboard.api.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private CompanyRepository companyRepository;
    @Mock private BCryptPasswordEncoder passwordEncoder;
    @Mock private JwtUtil jwtUtil;

    @InjectMocks private AuthService authService;

    private User applicantUser;
    private User companyUser;

    @BeforeEach
    void setUp() {
        applicantUser = new User();
        applicantUser.setId(1L);
        applicantUser.setEmail("applicant@test.com");
        applicantUser.setPassword("hashed");
        applicantUser.setRole(UserRole.APPLICANT);

        companyUser = new User();
        companyUser.setId(2L);
        companyUser.setEmail("company@test.com");
        companyUser.setPassword("hashed");
        companyUser.setRole(UserRole.COMPANY);
        companyUser.setCompanyId(10L);
    }

    // --- register ---

    @Test
    void register_applicant_savesUserAndReturnsToken() {
        RegisterRequest req = new RegisterRequest();
        req.setEmail("new@test.com");
        req.setPassword("password");
        req.setRole(UserRole.APPLICANT);

        when(userRepository.findByEmail("new@test.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(jwtUtil.generateToken(any(User.class))).thenReturn("jwt-token");

        AuthResponse response = authService.register(req);

        assertThat(response.getToken()).isEqualTo("jwt-token");
        assertThat(response.getRole()).isEqualTo("APPLICANT");
        verify(companyRepository, never()).save(any());
    }

    @Test
    void register_company_createsCompanyAndLinksId() {
        RegisterRequest req = new RegisterRequest();
        req.setEmail("co@test.com");
        req.setPassword("pass");
        req.setRole(UserRole.COMPANY);
        req.setCompanyName("Acme");
        req.setCompanyIndustry("Tech");
        req.setCompanySize("50");
        req.setCompanyWebsite("acme.com");

        Company savedCompany = new Company();
        savedCompany.setId(99L);
        savedCompany.setName("Acme");

        when(userRepository.findByEmail("co@test.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("pass")).thenReturn("hashed");
        when(companyRepository.save(any(Company.class))).thenReturn(savedCompany);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(jwtUtil.generateToken(any(User.class))).thenReturn("jwt-token");

        AuthResponse response = authService.register(req);

        assertThat(response.getRole()).isEqualTo("COMPANY");
        verify(companyRepository).save(any(Company.class));
        verify(userRepository).save(argThat(u -> u.getCompanyId().equals(99L)));
    }

    @Test
    void register_duplicateEmail_throwsRuntimeException() {
        RegisterRequest req = new RegisterRequest();
        req.setEmail("applicant@test.com");
        req.setRole(UserRole.APPLICANT);

        when(userRepository.findByEmail("applicant@test.com")).thenReturn(Optional.of(applicantUser));

        assertThatThrownBy(() -> authService.register(req))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Email already registered");

        verify(userRepository, never()).save(any());
    }

    // --- login ---

    @Test
    void login_validCredentials_returnsToken() {
        LoginRequest req = new LoginRequest();
        req.setEmail("applicant@test.com");
        req.setPassword("plaintext");

        when(userRepository.findByEmail("applicant@test.com")).thenReturn(Optional.of(applicantUser));
        when(passwordEncoder.matches("plaintext", "hashed")).thenReturn(true);
        when(jwtUtil.generateToken(applicantUser)).thenReturn("jwt-token");

        AuthResponse response = authService.login(req);

        assertThat(response.getToken()).isEqualTo("jwt-token");
        assertThat(response.getRole()).isEqualTo("APPLICANT");
    }

    @Test
    void login_userNotFound_throwsRuntimeException() {
        LoginRequest req = new LoginRequest();
        req.setEmail("nobody@test.com");
        req.setPassword("pass");

        when(userRepository.findByEmail("nobody@test.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Invalid credentials");
    }

    @Test
    void login_wrongPassword_throwsRuntimeException() {
        LoginRequest req = new LoginRequest();
        req.setEmail("applicant@test.com");
        req.setPassword("wrong");

        when(userRepository.findByEmail("applicant@test.com")).thenReturn(Optional.of(applicantUser));
        when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Invalid credentials");
    }
}
