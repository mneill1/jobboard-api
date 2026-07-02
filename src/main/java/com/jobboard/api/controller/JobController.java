package com.jobboard.api.controller;

import com.jobboard.api.dto.ApplicationRequest;
import com.jobboard.api.dto.ApplicationResponse;
import com.jobboard.api.dto.JobRequest;
import com.jobboard.api.dto.JobResponse;
import com.jobboard.api.service.ApplicationService;
import com.jobboard.api.service.JobService;
import com.jobboard.api.entity.JobStatus;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import java.util.List;

//import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/jobs") @RequiredArgsConstructor
public class JobController {
    private final JobService jobService;

    @PostMapping
    public ResponseEntity<JobResponse> create(@Valid @RequestBody JobRequest req) {
        return ResponseEntity.status(201).body(jobService.createJob(req));
    }

    @GetMapping
    public ResponseEntity<List<JobResponse>> list(
        @RequestParam(required = false) JobStatus status,
        @RequestParam(required = false) String location
    ) {
            return ResponseEntity.ok(jobService.list(status, location));
    }
    

    @GetMapping("/{id}")
    public ResponseEntity<JobResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(jobService.getById(id));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<JobResponse> updateStatus(
            @PathVariable Long id, @RequestParam JobStatus status) {
        return ResponseEntity.ok(jobService.updateStatus(id, status));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
            jobService.delete(id);
        return ResponseEntity.noContent().build();
    }

    private final ApplicationService applicationService;

    @PostMapping("/{id}/apply")
    public ResponseEntity<ApplicationResponse> apply(
            @PathVariable Long id,
            @Valid @RequestBody ApplicationRequest req){
        return ResponseEntity.status(201).body(applicationService.apply(id,req));
    }

    @GetMapping("/{id}/applications")
    public ResponseEntity<List<ApplicationResponse>> getApplications(@PathVariable Long id){
        return ResponseEntity.ok(applicationService.getByJobId(id));
    }

    @GetMapping("/search")
    public ResponseEntity<List<JobResponse>> search(@RequestParam String query){
        return ResponseEntity.ok(jobService.search(query));
    }
}

