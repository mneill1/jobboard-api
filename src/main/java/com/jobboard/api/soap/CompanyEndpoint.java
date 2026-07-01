package com.jobboard.api.soap;

import com.jobboard.api.entity.Company;
import com.jobboard.api.exception.ResourceNotFoundException;
import com.jobboard.api.repository.CompanyRepository;
import com.jobboard.hr.GetCompanyInfoRequest;
import com.jobboard.hr.GetCompanyInfoResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;
import org.springframework.ws.server.endpoint.annotation.RequestPayload;
import org.springframework.ws.server.endpoint.annotation.ResponsePayload;

@Endpoint
@RequiredArgsConstructor
public class CompanyEndpoint {

    private static final String NAMESPACE = "http://jobboard.com/hr";
    private final CompanyRepository companyRepo;

    @PayloadRoot(namespace = NAMESPACE, localPart = "getCompanyInfoRequest")
    @ResponsePayload
    public GetCompanyInfoResponse getCompanyInfo(
        @RequestPayload GetCompanyInfoRequest request) {
        Company company = companyRepo.findById(request.getCompanyId())
            .orElseThrow(() -> new ResourceNotFoundException("Company not found"));
        
        GetCompanyInfoResponse response = new GetCompanyInfoResponse();
        response.setId(company.getId());
        response.setName(company.getName());
        response.setIndustry(company.getIndustry());
        response.setSize(company.getSize());
        response.setWebsite(company.getWebsite());

        return response;
    }
}
