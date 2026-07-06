package com.jobboard.api.service;

import com.jobboard.api.dto.ApplicationRequest;
import com.jobboard.api.dto.ApplicationResponse;
import com.jobboard.api.entity.Application;
import com.jobboard.api.entity.Job;
import com.jobboard.api.exception.ResourceNotFoundException;
import com.jobboard.api.repository.ApplicationRepository;
import com.jobboard.api.repository.JobRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApplicationServiceTest {

    @Mock private ApplicationRepository applicationRepo;
    @Mock private JobRepository jobRepo;

    @InjectMocks private ApplicationService applicationService;

    private Job job;
    private Application application;

    @BeforeEach
    void setUp() {
        job = new Job();
        job.setId(1L);

        application = new Application();
        application.setId("app-1");
        application.setJobId(1L);
        application.setUserId(42L);
        application.setApplicantName("Alice");
        application.setEmail("alice@test.com");
        application.setResumeText("My CV");
        application.setStatus("RECEIVED");
    }

    // --- apply ---

    @Test
    void apply_validJobAndUser_savesApplicationWithUserId() {
        ApplicationRequest req = new ApplicationRequest();
        req.setApplicantName("Alice");
        req.setEmail("alice@test.com");
        req.setResumeText("My CV");

        when(jobRepo.findById(1L)).thenReturn(Optional.of(job));
        when(applicationRepo.save(any(Application.class))).thenReturn(application);

        ApplicationResponse response = applicationService.apply(1L, req, 42L);

        assertThat(response.getApplicantName()).isEqualTo("Alice");
        assertThat(response.getEmail()).isEqualTo("alice@test.com");
        assertThat(response.getStatus()).isEqualTo("RECEIVED");
        verify(applicationRepo).save(argThat(a -> a.getUserId().equals(42L)));
    }

    @Test
    void apply_nullUserId_savesApplicationWithNullUserId() {
        ApplicationRequest req = new ApplicationRequest();
        req.setApplicantName("Bob");
        req.setEmail("bob@test.com");

        Application savedApp = new Application();
        savedApp.setId("app-2");
        savedApp.setJobId(1L);
        savedApp.setApplicantName("Bob");
        savedApp.setEmail("bob@test.com");
        savedApp.setStatus("RECEIVED");

        when(jobRepo.findById(1L)).thenReturn(Optional.of(job));
        when(applicationRepo.save(any(Application.class))).thenReturn(savedApp);

        ApplicationResponse response = applicationService.apply(1L, req, null);

        assertThat(response.getApplicantName()).isEqualTo("Bob");
        verify(applicationRepo).save(argThat(a -> a.getUserId() == null));
    }

    @Test
    void apply_jobNotFound_throwsResourceNotFoundException() {
        ApplicationRequest req = new ApplicationRequest();
        req.setApplicantName("Alice");
        req.setEmail("alice@test.com");

        when(jobRepo.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> applicationService.apply(99L, req, 42L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Job not Found");

        verify(applicationRepo, never()).save(any());
    }

    // --- getByJobId ---

    @Test
    void getByJobId_returnsAllApplicationsForJob() {
        Application second = new Application();
        second.setId("app-2");
        second.setJobId(1L);
        second.setApplicantName("Bob");
        second.setEmail("bob@test.com");
        second.setStatus("RECEIVED");

        when(applicationRepo.findByJobId(1L)).thenReturn(List.of(application, second));

        List<ApplicationResponse> results = applicationService.getByJobId(1L);

        assertThat(results).hasSize(2);
        assertThat(results).extracting(ApplicationResponse::getApplicantName)
                .containsExactly("Alice", "Bob");
    }

    @Test
    void getByJobId_noApplications_returnsEmptyList() {
        when(applicationRepo.findByJobId(1L)).thenReturn(List.of());

        List<ApplicationResponse> results = applicationService.getByJobId(1L);

        assertThat(results).isEmpty();
    }
}
