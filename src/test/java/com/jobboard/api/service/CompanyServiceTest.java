package com.jobboard.api.service;

import com.jobboard.api.dto.CompanyRequest;
import com.jobboard.api.dto.CompanyResponse;
import com.jobboard.api.entity.Company;
import com.jobboard.api.exception.ResourceNotFoundException;
import com.jobboard.api.repository.CompanyRepository;
import com.jobboard.api.soap.HrSoapClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CompanyServiceTest {

    @Mock private CompanyRepository companyRepo;
    @Mock private HrSoapClient hrSoapClient;

    @InjectMocks private CompanyService companyService;

    private Company company;

    @BeforeEach
    void setUp() {
        company = new Company();
        company.setId(1L);
        company.setName("Acme");
        company.setIndustry("Tech");
        company.setSize("100");
        company.setWebsite("acme.com");
    }

    // --- create ---

    @Test
    void create_validRequest_savesAndReturnsResponse() {
        CompanyRequest req = new CompanyRequest();
        req.setName("Acme");
        req.setIndustry("Tech");
        req.setSize("100");
        req.setWebsite("acme.com");

        when(companyRepo.save(any(Company.class))).thenReturn(company);

        CompanyResponse response = companyService.create(req);

        assertThat(response.getName()).isEqualTo("Acme");
        assertThat(response.getIndustry()).isEqualTo("Tech");
        verify(companyRepo).save(any(Company.class));
    }

    // --- getById ---

    @Test
    void getById_existingId_returnsResponse() {
        when(companyRepo.findById(1L)).thenReturn(Optional.of(company));

        CompanyResponse response = companyService.getById(1L);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getName()).isEqualTo("Acme");
    }

    @Test
    void getById_missingId_throwsResourceNotFoundException() {
        when(companyRepo.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> companyService.getById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Company not found");
    }

    // --- getAll ---

    @Test
    void getAll_returnsAllCompanies() {
        Company second = new Company();
        second.setId(2L);
        second.setName("Beta Corp");

        when(companyRepo.findAll()).thenReturn(List.of(company, second));

        List<CompanyResponse> results = companyService.getAll();

        assertThat(results).hasSize(2);
        assertThat(results).extracting(CompanyResponse::getName)
                .containsExactly("Acme", "Beta Corp");
    }

    // --- update ---

    @Test
    void update_existingId_updatesAndReturnsResponse() {
        CompanyRequest req = new CompanyRequest();
        req.setName("Acme Updated");
        req.setIndustry("Finance");
        req.setSize("200");
        req.setWebsite("acme-new.com");

        Company updated = new Company();
        updated.setId(1L);
        updated.setName("Acme Updated");
        updated.setIndustry("Finance");

        when(companyRepo.findById(1L)).thenReturn(Optional.of(company));
        when(companyRepo.save(any(Company.class))).thenReturn(updated);

        CompanyResponse response = companyService.update(1L, req);

        assertThat(response.getName()).isEqualTo("Acme Updated");
        verify(companyRepo).save(company);
    }

    @Test
    void update_missingId_throwsResourceNotFoundException() {
        when(companyRepo.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> companyService.update(99L, new CompanyRequest()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Company not found");
    }

    // --- delete ---

    @Test
    void delete_existingId_deletesCompany() {
        when(companyRepo.existsById(1L)).thenReturn(true);

        companyService.delete(1L);

        verify(companyRepo).deleteById(1L);
    }

    @Test
    void delete_missingId_throwsResourceNotFoundException() {
        when(companyRepo.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> companyService.delete(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Company not found");

        verify(companyRepo, never()).deleteById(any());
    }
}
