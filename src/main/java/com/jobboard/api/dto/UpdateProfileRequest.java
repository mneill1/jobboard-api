package com.jobboard.api.dto;

import lombok.Data;

@Data
public class UpdateProfileRequest {
    private String email;
    private String password;
}
