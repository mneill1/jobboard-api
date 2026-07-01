package com.jobboard.api.controller;

import com.jobboard.api.dto.CompanyInfoDto;
import com.jobboard.api.dto.CompanyRequest;
import com.jobboard.api.dto.CompanyResponse;
import com.jobboard.api.service.CompanyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/companies")
@RequiredArgsConstructor
public class CompanyController {

    private final CompanyService companyService;

    @PostMapping
    public ResponseEntity<CompanyResponse> create(@Valid @RequestBody CompanyRequest req) {
        return ResponseEntity.status(201).body(companyService.create(req));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CompanyResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(companyService.getById(id));
    }

    @GetMapping
    public ResponseEntity<List<CompanyResponse>> getAll() {
        return ResponseEntity.ok(companyService.getAll());
    }

    @PutMapping("/{id}")
    public ResponseEntity<CompanyResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody CompanyRequest req) {
        return ResponseEntity.ok(companyService.update(id, req));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        companyService.delete(id);
        return ResponseEntity.noContent().build();
    }
    @GetMapping("/{id}/hr-info")
    public ResponseEntity<CompanyInfoDto> getHrInfo(@PathVariable Long id) {
        return ResponseEntity.ok(companyService.getHrInfo(id));
    }
}