package com.jobboard.api.soap;

import com.jobboard.hr.GetCompanyInfoRequest;
import com.jobboard.hr.GetCompanyInfoResponse;
import org.springframework.stereotype.Component;
import org.springframework.ws.client.core.support.WebServiceGatewaySupport;

@Component
public class HrSoapClient extends WebServiceGatewaySupport {

    public GetCompanyInfoResponse getCompanyInfo(Long companyId) {
        GetCompanyInfoRequest request = new GetCompanyInfoRequest();
        request.setCompanyId(companyId);

        return (GetCompanyInfoResponse) getWebServiceTemplate()
            .marshalSendAndReceive(
                "http://localhost:8080/ws",
                request
            );
    }
}