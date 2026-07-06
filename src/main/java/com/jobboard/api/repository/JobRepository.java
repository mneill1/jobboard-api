package com.jobboard.api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import com.jobboard.api.entity.Job;
import com.jobboard.api.entity.JobStatus;
import java.util.List;

public interface JobRepository extends JpaRepository<Job, Long>, JpaSpecificationExecutor<Job> {
    List<Job> findByStatus(JobStatus status);
    List<Job> findByLocationContainingIgnoreCase(String location);
    List<Job> findByCompany_Id(Long companyId);
}