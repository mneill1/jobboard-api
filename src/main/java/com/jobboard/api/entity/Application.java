package com.jobboard.api.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "applications")
@Data
public class Application {
    @Id
    private String id;
    private Long jobId;
    private String applicantName;
    private String email;
    private String resumeText;
    private String status = "RECEIVED";
    private LocalDateTime appliedAt = LocalDateTime.now();
    private List<String> notes = new ArrayList<>();
}
