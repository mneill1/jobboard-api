package com.jobboard.api.dto;

import com.jobboard.api.entity.JobStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class JobResponse {

    private Long id;
    private String title;
    private String description;
    private String location;
    private Integer salaryMin;
    private Integer salaryMax;
    private JobStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private Long companyId;
    private String companyName;
    private String companyLogoUrl;
}