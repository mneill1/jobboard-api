package com.jobboard.api.service;

import com.jobboard.api.dto.ApplicationResponse;
import com.jobboard.api.dto.CompanyResponse;
import com.jobboard.api.dto.JobResponse;
import com.jobboard.api.dto.UpdateProfileRequest;
import com.jobboard.api.dto.UserProfileResponse;
import com.jobboard.api.entity.Application;
import com.jobboard.api.entity.Company;
import com.jobboard.api.entity.Job;
import com.jobboard.api.entity.User;
import com.jobboard.api.repository.ApplicationRepository;
import com.jobboard.api.repository.CompanyRepository;
import com.jobboard.api.repository.JobRepository;
import com.jobboard.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final ApplicationRepository applicationRepository;
    private final JobRepository jobRepository;
    private final CompanyRepository companyRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public UserProfileResponse getProfile(User user) {
        UserProfileResponse res = new UserProfileResponse();
        res.setId(user.getId());
        res.setEmail(user.getEmail());
        res.setRole(user.getRole().name());
        res.setCompanyId(user.getCompanyId());
        res.setCreatedAt(user.getCreatedAt());

        if (user.getCompanyId() != null) {
            companyRepository.findById(user.getCompanyId()).ifPresent(company -> {
                res.setCompany(toCompanyResponse(company));
            });
        }
        return res;
    }

    public List<ApplicationResponse> getApplications(User user) {
        return applicationRepository.findByUserId(user.getId()).stream()
                .map(this::toApplicationResponse)
                .toList();
    }

    public List<JobResponse> getJobs(User user) {
        return jobRepository.findByCompany_Id(user.getCompanyId()).stream()
                .map(this::toJobResponse)
                .toList();
    }

    public UserProfileResponse updateProfile(User user, UpdateProfileRequest req) {
        if (req.getEmail() != null) {
            userRepository.findByEmail(req.getEmail()).ifPresent(existing -> {
                if (!existing.getId().equals(user.getId())) {
                    throw new RuntimeException("Email already in use");
                }
            });
            user.setEmail(req.getEmail());
        }
        if (req.getPassword() != null) {
            user.setPassword(passwordEncoder.encode(req.getPassword()));
        }
        return getProfile(userRepository.save(user));
    }

    private CompanyResponse toCompanyResponse(Company company) {
        CompanyResponse res = new CompanyResponse();
        res.setId(company.getId());
        res.setName(company.getName());
        res.setIndustry(company.getIndustry());
        res.setSize(company.getSize());
        res.setWebsite(company.getWebsite());
        if (company.getLogoPath() != null) {
            res.setLogoUrl("/api/companies/" + company.getId() + "/logo");
        }
        res.setCreatedAt(company.getCreatedAt());
        return res;
    }

    private ApplicationResponse toApplicationResponse(Application app) {
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

    private JobResponse toJobResponse(Job job) {
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
        if (job.getCompany().getLogoPath() != null) {
            res.setCompanyLogoUrl("/api/companies/" + job.getCompany().getId() + "/logo");
        }
        return res;
    }
}
