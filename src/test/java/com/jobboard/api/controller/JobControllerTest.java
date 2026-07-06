package com.jobboard.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobboard.api.TestSecurityConfig;
import com.jobboard.api.config.JwtUtil;
import com.jobboard.api.config.UserDetailsServiceImpl;
import com.jobboard.api.dto.JobFilterRequest;
import com.jobboard.api.dto.JobRequest;
import com.jobboard.api.dto.JobResponse;
import com.jobboard.api.entity.JobStatus;
import org.mockito.ArgumentCaptor;
import com.jobboard.api.exception.ResourceNotFoundException;
import com.jobboard.api.repository.UserRepository;
import com.jobboard.api.service.ApplicationService;
import com.jobboard.api.service.JobService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(JobController.class)
@Import(TestSecurityConfig.class)
class JobControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private JobService jobService;
    @MockBean private ApplicationService applicationService;
    @MockBean private JwtUtil jwtUtil;
    @MockBean private UserDetailsServiceImpl userDetailsService;
    @MockBean private UserRepository userRepository;

    private JobResponse jobResponse;

    @BeforeEach
    void setUp() {
        jobResponse = new JobResponse();
        jobResponse.setId(1L);
        jobResponse.setTitle("Engineer");
        jobResponse.setStatus(JobStatus.DRAFT);
        jobResponse.setCompanyId(10L);
        jobResponse.setCompanyName("Acme");
    }

    // --- GET /api/jobs ---

    @Test
    void listJobs_noFilters_returnsOk() throws Exception {
        when(jobService.list(any(JobFilterRequest.class))).thenReturn(List.of(jobResponse));

        mockMvc.perform(get("/api/jobs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Engineer"));
    }

    @Test
    void listJobs_filterByStatus_passesStatusToService() throws Exception {
        when(jobService.list(any(JobFilterRequest.class))).thenReturn(List.of());

        mockMvc.perform(get("/api/jobs").param("status", "ACTIVE"))
                .andExpect(status().isOk());

        ArgumentCaptor<JobFilterRequest> captor = ArgumentCaptor.forClass(JobFilterRequest.class);
        verify(jobService).list(captor.capture());
        org.assertj.core.api.Assertions.assertThat(captor.getValue().getStatus()).isEqualTo(JobStatus.ACTIVE);
    }

    // --- GET /api/jobs/{id} ---

    @Test
    void getById_existingJob_returnsOk() throws Exception {
        when(jobService.getById(1L)).thenReturn(jobResponse);

        mockMvc.perform(get("/api/jobs/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Engineer"));
    }

    @Test
    void getById_missingJob_returns400() throws Exception {
        when(jobService.getById(99L)).thenThrow(new ResourceNotFoundException("Job not found"));

        mockMvc.perform(get("/api/jobs/99"))
                .andExpect(status().isBadRequest());
    }

    // --- POST /api/jobs ---

    @Test
    void createJob_validRequest_returns201() throws Exception {
        JobRequest req = new JobRequest();
        req.setTitle("Engineer");
        req.setCompanyId(10L);

        when(jobService.createJob(any(JobRequest.class))).thenReturn(jobResponse);

        mockMvc.perform(post("/api/jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Engineer"));
    }

    @Test
    void createJob_missingTitle_returns400() throws Exception {
        JobRequest req = new JobRequest();
        req.setCompanyId(10L);

        mockMvc.perform(post("/api/jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    // --- DELETE /api/jobs/{id} ---

    @Test
    void deleteJob_existingId_returns204() throws Exception {
        doNothing().when(jobService).delete(1L);

        mockMvc.perform(delete("/api/jobs/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteJob_missingId_returns400() throws Exception {
        doThrow(new ResourceNotFoundException("Job not found")).when(jobService).delete(99L);

        mockMvc.perform(delete("/api/jobs/99"))
                .andExpect(status().isBadRequest());
    }

    // --- PUT /api/jobs/{id}/status ---

    @Test
    void updateStatus_validRequest_returnsOk() throws Exception {
        when(jobService.updateStatus(1L, JobStatus.ACTIVE)).thenReturn(jobResponse);

        mockMvc.perform(put("/api/jobs/1/status").param("status", "ACTIVE"))
                .andExpect(status().isOk());

        verify(jobService).updateStatus(1L, JobStatus.ACTIVE);
    }

    // --- GET /api/jobs/search ---

    @Test
    void searchJobs_returnsResults() throws Exception {
        when(jobService.search("engineer")).thenReturn(List.of(jobResponse));

        mockMvc.perform(get("/api/jobs/search").param("query", "engineer"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Engineer"));
    }
}
