package com.jobboard.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class JobRequest {

    @NotBlank(message = "Title is required")
    private String title;

    private String description;

    @NotNull(message = "Company ID is required")
    private Long companyId;

    private String location;

    @Min(value = 0, message = "Salary must be positive")
    private Integer salaryMin;

    @Min(value = 0, message = "Salary must be positive")
    private Integer salaryMax;
}