package com.jobboard.api.dto;

import com.jobboard.api.entity.JobStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class JobFilterRequest {
    private String query;
    private String location;
    private Integer minSalary;
    private JobStatus status;
    private LocalDateTime postedAfter;
}
