package com.jobboard.api.controller;

import com.jobboard.api.dto.CompanyInfoDto;
import com.jobboard.api.dto.CompanyRequest;
import com.jobboard.api.dto.CompanyResponse;
import com.jobboard.api.service.CompanyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.nio.file.Paths;
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
    @PostMapping("/{id}/logo")
    public ResponseEntity<CompanyResponse> uploadLogo(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(companyService.uploadLogo(id, file));
    }

    @GetMapping("/{id}/logo")
    public ResponseEntity<Resource> getLogo(
            @PathVariable Long id,
            @Value("${app.upload.dir:uploads}") String uploadDir) {
        try {
            CompanyResponse company = companyService.getById(id);
            if (company.getLogoUrl() == null) {
                return ResponseEntity.notFound().build();
            }
            // derive filename from the stored path pattern
            Path dir = Paths.get(uploadDir, "logos");
            // list files matching company-{id}-*
            java.io.File[] files = dir.toFile().listFiles(
                (d, name) -> name.startsWith("company-" + id + "-"));
            if (files == null || files.length == 0) {
                return ResponseEntity.notFound().build();
            }
            Path file = files[files.length - 1].toPath();
            Resource resource = new UrlResource(file.toUri());
            String contentType = java.nio.file.Files.probeContentType(file);
            if (contentType == null) contentType = "application/octet-stream";
            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                .contentType(MediaType.parseMediaType(contentType))
                .body(resource);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{id}/hr-info")
    public ResponseEntity<CompanyInfoDto> getHrInfo(@PathVariable Long id) {
        return ResponseEntity.ok(companyService.getHrInfo(id));
    }
}