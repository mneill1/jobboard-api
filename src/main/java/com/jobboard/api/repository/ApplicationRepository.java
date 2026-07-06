package com.jobboard.api.repository;

import com.jobboard.api.entity.Application;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ApplicationRepository extends MongoRepository<Application, String>{
    List<Application> findByJobId(Long jobId);
    List<Application> findByUserId(Long userId);
}
