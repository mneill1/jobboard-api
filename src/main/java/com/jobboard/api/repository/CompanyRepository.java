package com.jobboard.api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.jobboard.api.entity.Company; // or Job

public interface CompanyRepository extends JpaRepository<Company, Long> {}