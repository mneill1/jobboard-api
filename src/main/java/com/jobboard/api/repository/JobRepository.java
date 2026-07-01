package com.jobboard.api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.jobboard.api.entity.Job;
import com.jobboard.api.entity.JobStatus;
import java.util.List;

public interface JobRepository extends JpaRepository<Job, Long> {
    List<Job> findByStatus(JobStatus status);
    List<Job> findByLocationContainingIgnoreCase(String location);
}