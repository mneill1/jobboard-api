package com.jobboard.api.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ApplicationResponse {
    private String id;
    private Long jobId;
    private String applicantName;
    private String email;
    private String resumeText;
    private String status;
    private LocalDateTime appliedAt;
    private List<String> notes;
}
