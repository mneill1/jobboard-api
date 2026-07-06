package com.jobboard.api.service;

import com.jobboard.api.dto.JobRequest;
import com.jobboard.api.dto.JobResponse;
import com.jobboard.api.entity.Company;
import com.jobboard.api.entity.Job;
import com.jobboard.api.entity.JobDocument;
import com.jobboard.api.entity.JobStatus;
import com.jobboard.api.exception.ResourceNotFoundException;
import com.jobboard.api.repository.CompanyRepository;
import com.jobboard.api.repository.JobRepository;
import com.jobboard.api.repository.JobSearchRepository;
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
class JobServiceTest {

    @Mock private JobRepository jobRepo;
    @Mock private CompanyRepository companyRepo;
    @Mock private JobSearchRepository jobSearchRepo;

    // JobService uses manual constructor injection — build it manually
    private JobService jobService;

    private Company company;
    private Job job;

    @BeforeEach
    void setUp() {
        jobService = new JobService(jobRepo, companyRepo, jobSearchRepo);

        company = new Company();
        company.setId(1L);
        company.setName("Acme");

        job = new Job();
        job.setId(10L);
        job.setTitle("Engineer");
        job.setDescription("Build things");
        job.setLocation("London");
        job.setSalaryMin(50000);
        job.setSalaryMax(80000);
        job.setStatus(JobStatus.DRAFT);
        job.setCompany(company);
    }

    // --- createJob ---

    @Test
    void createJob_validRequest_returnsJobResponse() {
        JobRequest req = new JobRequest();
        req.setTitle("Engineer");
        req.setCompanyId(1L);
        req.setLocation("London");

        when(companyRepo.findById(1L)).thenReturn(Optional.of(company));
        when(jobRepo.save(any(Job.class))).thenReturn(job);

        JobResponse response = jobService.createJob(req);

        assertThat(response.getTitle()).isEqualTo("Engineer");
        assertThat(response.getCompanyName()).isEqualTo("Acme");
        assertThat(response.getStatus()).isEqualTo(JobStatus.DRAFT);
    }

    @Test
    void createJob_companyNotFound_throwsResourceNotFoundException() {
        JobRequest req = new JobRequest();
        req.setTitle("Engineer");
        req.setCompanyId(99L);

        when(companyRepo.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> jobService.createJob(req))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Company not found");
    }

    // --- getById ---

    @Test
    void getById_existingId_returnsJobResponse() {
        when(jobRepo.findById(10L)).thenReturn(Optional.of(job));

        JobResponse response = jobService.getById(10L);

        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getTitle()).isEqualTo("Engineer");
    }

    @Test
    void getById_missingId_throwsResourceNotFoundException() {
        when(jobRepo.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> jobService.getById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Job not found");
    }

    // --- list ---

    @Test
    void list_noFilters_returnsAllJobs() {
        when(jobRepo.findAll()).thenReturn(List.of(job));

        List<JobResponse> results = jobService.list(null, null);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getTitle()).isEqualTo("Engineer");
    }

    @Test
    void list_filterByStatus_delegatesToFindByStatus() {
        when(jobRepo.findByStatus(JobStatus.ACTIVE)).thenReturn(List.of(job));

        List<JobResponse> results = jobService.list(JobStatus.ACTIVE, null);

        assertThat(results).hasSize(1);
        verify(jobRepo).findByStatus(JobStatus.ACTIVE);
        verify(jobRepo, never()).findAll();
    }

    @Test
    void list_filterByLocation_delegatesToFindByLocation() {
        when(jobRepo.findByLocationContainingIgnoreCase("London")).thenReturn(List.of(job));

        List<JobResponse> results = jobService.list(null, "London");

        assertThat(results).hasSize(1);
        verify(jobRepo).findByLocationContainingIgnoreCase("London");
    }

    // --- delete ---

    @Test
    void delete_existingId_deletesJob() {
        when(jobRepo.existsById(10L)).thenReturn(true);

        jobService.delete(10L);

        verify(jobRepo).deleteById(10L);
    }

    @Test
    void delete_missingId_throwsResourceNotFoundException() {
        when(jobRepo.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> jobService.delete(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Job not found");

        verify(jobRepo, never()).deleteById(any());
    }

    // --- updateStatus ---

    @Test
    void updateStatus_toDraft_savesAndDoesNotIndexElasticsearch() {
        when(jobRepo.findById(10L)).thenReturn(Optional.of(job));
        when(jobRepo.save(any(Job.class))).thenReturn(job);

        jobService.updateStatus(10L, JobStatus.DRAFT);

        verify(jobSearchRepo, never()).save(any(JobDocument.class));
    }

    @Test
    void updateStatus_toActive_savesAndIndexesElasticsearch() {
        job.setStatus(JobStatus.DRAFT);
        when(jobRepo.findById(10L)).thenReturn(Optional.of(job));
        when(jobRepo.save(any(Job.class))).thenReturn(job);

        jobService.updateStatus(10L, JobStatus.ACTIVE);

        verify(jobSearchRepo).save(any(JobDocument.class));
    }

    @Test
    void updateStatus_missingId_throwsResourceNotFoundException() {
        when(jobRepo.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> jobService.updateStatus(99L, JobStatus.ACTIVE))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Job not found");
    }

    // --- search ---

    @Test
    void search_returnsMatchingJobsViaElasticsearch() {
        JobDocument doc = new JobDocument();
        doc.setId("10");

        when(jobSearchRepo.findByTitleContainingOrDescriptionContaining("eng", "eng"))
                .thenReturn(List.of(doc));
        when(jobRepo.findById(10L)).thenReturn(Optional.of(job));

        List<JobResponse> results = jobService.search("eng");

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getId()).isEqualTo(10L);
    }
}
