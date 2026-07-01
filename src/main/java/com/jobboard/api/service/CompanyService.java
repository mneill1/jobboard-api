package com.jobboard.api.service;

import com.jobboard.api.dto.CompanyInfoDto;
import com.jobboard.api.dto.CompanyRequest;
import com.jobboard.api.dto.CompanyResponse;
import com.jobboard.api.entity.Company;
import com.jobboard.api.exception.ResourceNotFoundException;
import com.jobboard.api.repository.CompanyRepository;
import com.jobboard.api.soap.HrSoapClient;
import com.jobboard.hr.GetCompanyInfoResponse;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CompanyService {

    private final CompanyRepository companyRepo;

    public CompanyResponse create(CompanyRequest req) {
        Company company = new Company();
        company.setName(req.getName());
        company.setIndustry(req.getIndustry());
        company.setSize(req.getSize());
        company.setWebsite(req.getWebsite());
        return toResponse(companyRepo.save(company));
    }

    public CompanyResponse getById(Long id) {
        Company company = companyRepo.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Company not found"));
        return toResponse(company);
    }

    public List<CompanyResponse> getAll() {
        return companyRepo.findAll().stream()
            .map(this::toResponse)
            .toList();
    }

    public CompanyResponse update(Long id, CompanyRequest req) {
        Company company = companyRepo.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Company not found"));
        company.setName(req.getName());
        company.setIndustry(req.getIndustry());
        company.setSize(req.getSize());
        company.setWebsite(req.getWebsite());
        return toResponse(companyRepo.save(company));
    }

    public void delete(Long id) {
        if (!companyRepo.existsById(id)) {
            throw new ResourceNotFoundException("Company not found");
        }
        companyRepo.deleteById(id);
    }

    private CompanyResponse toResponse(Company company) {
        CompanyResponse res = new CompanyResponse();
        res.setId(company.getId());
        res.setName(company.getName());
        res.setIndustry(company.getIndustry());
        res.setSize(company.getSize());
        res.setWebsite(company.getWebsite());
        res.setCreatedAt(company.getCreatedAt());
        return res;
    }
    private final HrSoapClient hrSoapClient;

    public CompanyInfoDto getHrInfo(Long id) {
        GetCompanyInfoResponse response = hrSoapClient.getCompanyInfo(id);
        CompanyInfoDto dto = new CompanyInfoDto();
        dto.setId(response.getId());
        dto.setName(response.getName());
        dto.setIndustry(response.getIndustry());
        dto.setSize(response.getSize());
        dto.setWebsite(response.getWebsite());
        return dto;
}
}