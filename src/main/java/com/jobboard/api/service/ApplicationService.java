package com.jobboard.api.service;

import com.jobboard.api.dto.ApplicationRequest;
import com.jobboard.api.dto.ApplicationResponse;
import com.jobboard.api.entity.Application;
import com.jobboard.api.exception.ResourceNotFoundException;
import com.jobboard.api.repository.ApplicationRepository;
import com.jobboard.api.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ApplicationService {
    private final ApplicationRepository applicationRepo;
    private final JobRepository jobRepo;

    public ApplicationResponse apply(Long jobId, ApplicationRequest req, Long userId){
        jobRepo.findById(jobId)
            .orElseThrow(() -> new ResourceNotFoundException("Job not Found"));

            Application app = new Application();
            app.setJobId(jobId);
            app.setUserId(userId);
            app.setApplicantName(req.getApplicantName());
            app.setEmail(req.getEmail());
            app.setResumeText(req.getResumeText());
            return toResponse(applicationRepo.save(app));
    }

    public List<ApplicationResponse> getByJobId(Long jobId){
        return applicationRepo.findByJobId(jobId).stream()
            .map(this::toResponse)
            .toList();
    }

    private ApplicationResponse toResponse(Application app){
        ApplicationResponse res = new ApplicationResponse();

        res.setId(app.getId());
        res.setJobId(app.getJobId());
        res.setApplicantName(app.getApplicantName());
        res.setEmail(app.getEmail());
        res.setResumeText(app.getResumeText());
        res.setStatus(app.getStatus());
        res.setAppliedAt(app.getAppliedAt());
        res.setNotes(app.getNotes());

        return res;
    } 
}
