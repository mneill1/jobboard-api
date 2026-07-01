package com.jobboard.api.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity @Data @NoArgsConstructor
public class Job {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @NotBlank private String title;
    @Column(columnDefinition = "TEXT") private String description;
    @ManyToOne @JoinColumn(name = "company_id") private Company company;
    private String location;
    private Integer salaryMin;
    private Integer salaryMax;
    @Enumerated(EnumType.STRING)
    private JobStatus status = JobStatus.DRAFT;
    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt;
}