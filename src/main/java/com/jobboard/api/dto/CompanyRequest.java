package com.jobboard.api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CompanyRequest {

    @NotBlank(message = "Company name is required")
    private String name;

    private String industry;
    private String size;
    private String website;
}
