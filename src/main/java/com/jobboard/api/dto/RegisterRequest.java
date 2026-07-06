package com.jobboard.api.dto;

import com.jobboard.api.entity.UserRole;
import lombok.Data;

@Data
public class RegisterRequest {
    private String email;
    private String password;
    private UserRole role;
    private String companyName;
    private String companyIndustry;
    private String companySize;
    private String companyWebsite;
}
