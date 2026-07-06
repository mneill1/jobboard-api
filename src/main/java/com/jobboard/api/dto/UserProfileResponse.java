package com.jobboard.api.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class UserProfileResponse {
    private Long id;
    private String email;
    private String role;
    private Long companyId;
    private CompanyResponse company;
    private LocalDateTime createdAt;
}
