package com.jobboard.api.repository;

import com.jobboard.api.dto.JobFilterRequest;
import com.jobboard.api.entity.Job;
import com.jobboard.api.entity.JobStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public final class JobSpecifications {

    private JobSpecifications() {}

    public static Specification<Job> fromFilter(JobFilterRequest filter) {
        return (root, cq, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            String query = filter.getQuery();
            if (query != null && !query.isBlank()) {
                String like = "%" + query.toLowerCase() + "%";
                predicates.add(cb.or(
                    cb.like(cb.lower(root.get("title")), like),
                    cb.like(cb.lower(root.get("description")), like)
                ));
            }

            String location = filter.getLocation();
            if (location != null && !location.isBlank()) {
                predicates.add(cb.like(
                    cb.lower(root.get("location")),
                    "%" + location.toLowerCase() + "%"
                ));
            }

            Integer minSalary = filter.getMinSalary();
            if (minSalary != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("salaryMax"), minSalary));
            }

            JobStatus status = filter.getStatus();
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            LocalDateTime postedAfter = filter.getPostedAfter();
            if (postedAfter != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), postedAfter));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
