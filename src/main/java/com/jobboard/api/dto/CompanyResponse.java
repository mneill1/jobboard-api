package com.jobboard.api.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class CompanyResponse {
    private Long id;
    private String name;
    private String industry;
    private String size;
    private String website;
    private String logoUrl;
    private LocalDateTime createdAt;
}
