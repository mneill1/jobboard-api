package com.jobboard.api.repository;

import com.jobboard.api.entity.JobDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.util.List;

public interface JobSearchRepository extends ElasticsearchRepository<JobDocument, String>{
    List<JobDocument> findByTitleContainingOrDescriptionContaining(String title, String description);
}
