package com.jobboard.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobboard.api.TestSecurityConfig;
import com.jobboard.api.config.JwtUtil;
import com.jobboard.api.config.UserDetailsServiceImpl;
import com.jobboard.api.dto.ApplicationResponse;
import com.jobboard.api.dto.JobResponse;
import com.jobboard.api.dto.UpdateProfileRequest;
import com.jobboard.api.dto.UserProfileResponse;
import com.jobboard.api.entity.User;
import com.jobboard.api.entity.UserRole;
import com.jobboard.api.repository.UserRepository;
import com.jobboard.api.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@Import(TestSecurityConfig.class)
class UserControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private UserService userService;
    @MockBean private JwtUtil jwtUtil;
    @MockBean private UserDetailsServiceImpl userDetailsService;
    @MockBean private UserRepository userRepository;

    private User currentUser;
    private UserProfileResponse profileResponse;

    @BeforeEach
    void setUp() {
        currentUser = new User();
        currentUser.setId(1L);
        currentUser.setEmail("alice@test.com");
        currentUser.setRole(UserRole.APPLICANT);
        currentUser.setPassword("hashed");

        profileResponse = new UserProfileResponse();
        profileResponse.setId(1L);
        profileResponse.setEmail("alice@test.com");
        profileResponse.setRole("APPLICANT");
    }

    // --- GET /api/users/me ---

    @Test
    void getProfile_authenticated_returnsProfile() throws Exception {
        when(userService.getProfile(any(User.class))).thenReturn(profileResponse);

        mockMvc.perform(get("/api/users/me").with(user(currentUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("alice@test.com"))
                .andExpect(jsonPath("$.role").value("APPLICANT"));
    }

    // --- GET /api/users/me/applications ---

    @Test
    void getApplications_returnsApplicationList() throws Exception {
        ApplicationResponse app = new ApplicationResponse();
        app.setId("app-1");
        app.setApplicantName("Alice");
        app.setStatus("RECEIVED");

        when(userService.getApplications(any(User.class))).thenReturn(List.of(app));

        mockMvc.perform(get("/api/users/me/applications").with(user(currentUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].applicantName").value("Alice"));
    }

    // --- GET /api/users/me/jobs ---

    @Test
    void getJobs_companyUser_returnsJobList() throws Exception {
        User companyUser = new User();
        companyUser.setId(2L);
        companyUser.setEmail("co@test.com");
        companyUser.setRole(UserRole.COMPANY);
        companyUser.setPassword("hashed");
        companyUser.setCompanyId(10L);

        JobResponse job = new JobResponse();
        job.setId(5L);
        job.setTitle("Engineer");

        when(userService.getJobs(any(User.class))).thenReturn(List.of(job));

        mockMvc.perform(get("/api/users/me/jobs").with(user(companyUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Engineer"));
    }

    // --- PUT /api/users/me ---

    @Test
    void updateProfile_validRequest_returnsUpdatedProfile() throws Exception {
        UpdateProfileRequest req = new UpdateProfileRequest();
        req.setEmail("new@test.com");

        UserProfileResponse updated = new UserProfileResponse();
        updated.setId(1L);
        updated.setEmail("new@test.com");
        updated.setRole("APPLICANT");

        when(userService.updateProfile(any(User.class), any(UpdateProfileRequest.class)))
                .thenReturn(updated);

        mockMvc.perform(put("/api/users/me")
                        .with(user(currentUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("new@test.com"));
    }

    @Test
    void updateProfile_emailAlreadyInUse_returns400() throws Exception {
        UpdateProfileRequest req = new UpdateProfileRequest();
        req.setEmail("taken@test.com");

        when(userService.updateProfile(any(User.class), any(UpdateProfileRequest.class)))
                .thenThrow(new RuntimeException("Email already in use"));

        mockMvc.perform(put("/api/users/me")
                        .with(user(currentUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }
}
