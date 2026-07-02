package com.jobboard.api.service;

import com.jobboard.api.dto.JobRequest;
import com.jobboard.api.dto.JobResponse;
import com.jobboard.api.entity.JobStatus;
import com.jobboard.api.entity.Company;
import com.jobboard.api.entity.Job;
import com.jobboard.api.entity.JobDocument;
import com.jobboard.api.exception.ResourceNotFoundException;
import com.jobboard.api.repository.JobRepository;
import com.jobboard.api.repository.JobSearchRepository;

import lombok.extern.slf4j.Slf4j;

//import jakarta.persistence.criteria.JoinType;

import com.jobboard.api.repository.CompanyRepository;
//import lombok.RequiredArgsConstructor;

import org.springframework.context.annotation.Lazy;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service //@RequiredArgsConstructor
public class JobService {
    private final JobRepository jobRepo;
    private final CompanyRepository companyRepo;
    private final JobSearchRepository jobSearchRepo;

    public JobService(JobRepository jobRepo, CompanyRepository companyRepo, @Lazy JobSearchRepository jobSearchRepo){
        this.jobRepo = jobRepo;
        this.companyRepo = companyRepo;
        this.jobSearchRepo = jobSearchRepo;
    }

    public JobResponse createJob(JobRequest req) {
        Company company = companyRepo.findById(req.getCompanyId())
            .orElseThrow(() -> new ResourceNotFoundException("Company not found"));
        Job job = new Job();
        job.setTitle(req.getTitle());
        job.setDescription(req.getDescription());
        job.setCompany(company);
        job.setLocation(req.getLocation());
        job.setSalaryMin(req.getSalaryMin());
        job.setSalaryMax(req.getSalaryMax());
        return toResponse(jobRepo.save(job));
    }

    @Cacheable(value = "jobs", key = "#id")
    public JobResponse getById(Long id) {
        log.info("Fetching job id={}", id);
        Job job = jobRepo.findById(id)
            .orElseThrow(() -> {
                log.warn("Job not found id={}", id);
                return new ResourceNotFoundException("Job not found");
            });
        log.debug("Cache miss for job id={} — loaded from MySQL", id);
        return toResponse(job);
    }
    @Cacheable(value = "jobs", key = "#id")
    public List<JobResponse> list(JobStatus status, String location){
        if(status != null){
            return jobRepo.findByStatus(status).stream()
                .map(this::toResponse)
                .toList();
        }
        if(location != null){
            return jobRepo.findByLocationContainingIgnoreCase(location).stream()
                .map(this::toResponse)
                .toList();
        }
        return jobRepo.findAll().stream()
            .map(this::toResponse)
            .toList();
    }

    @CacheEvict(value = "jobs", key = "#id")
    public void delete(Long id) {
        if (!jobRepo.existsById(id)) {
            throw new ResourceNotFoundException("Job not found");
        }
        jobRepo.deleteById(id);
    }

    // @Lazy
    // private final JobSearchRepository jobSearchRepo;

    @CacheEvict(value = "jobs", key = "#id")
    public JobResponse updateStatus(Long id, JobStatus newStatus) {
        log.info("Updating job id={} status to {}", id, newStatus);
        Job job = jobRepo.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Job not found"));
        JobStatus oldStatus = job.getStatus();
        job.setStatus(newStatus);
        job.setUpdatedAt(LocalDateTime.now());
        Job saved = jobRepo.save(job);
        log.info("Job id={} status changed {} -> {}", id, oldStatus, newStatus);
        
        if (newStatus == JobStatus.ACTIVE){
            log.info("Indexing job id={} into Elasticsearch", id);
            JobDocument doc = new JobDocument();
            doc.setId(String.valueOf(saved.getId()));
            doc.setTitle(saved.getTitle());
            doc.setDescription(saved.getDescription());
            doc.setLocation(saved.getLocation());
            doc.setCompanyName(saved.getCompany().getName());
            doc.setStatus(saved.getStatus().name());
            jobSearchRepo.save(doc);
        }
        return toResponse(saved);
    }

    public List<JobResponse> search(String query){
        return jobSearchRepo
            .findByTitleContainingOrDescriptionContaining(query, query)
            .stream()
            .map(doc -> getById(Long.parseLong(doc.getId())))
            .toList();
    }

    private JobResponse toResponse(Job job) {
        JobResponse res = new JobResponse();
        res.setId(job.getId());
        res.setTitle(job.getTitle());
        res.setDescription(job.getDescription());
        res.setLocation(job.getLocation());
        res.setSalaryMin(job.getSalaryMin());
        res.setSalaryMax(job.getSalaryMax());
        res.setStatus(job.getStatus());
        res.setCreatedAt(job.getCreatedAt());
        res.setUpdatedAt(job.getUpdatedAt());
        
        res.setCompanyId(job.getCompany().getId());
        res.setCompanyName(job.getCompany().getName());
        return res;
     }
}