package com.jobboard.api.dto;

import lombok.Data;

@Data
public class CompanyInfoDto {
    private Long id;
    private String name;
    private String industry;
    private String size;
    private String website;
}