package com.sme.be_sme.modules.platform.processor.company;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.company.infrastructure.mapper.CompanyMapper;
import com.sme.be_sme.modules.company.infrastructure.persistence.entity.CompanyEntity;
import com.sme.be_sme.modules.platform.api.request.PlatformCompanySuspendRequest;
import com.sme.be_sme.modules.platform.api.response.PlatformCompanySuspendResponse;
import com.sme.be_sme.shared.constant.ErrorCodes;
import com.sme.be_sme.shared.exception.AppException;
import com.sme.be_sme.shared.gateway.core.BaseBizProcessor;
import com.sme.be_sme.shared.gateway.core.BizContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
@RequiredArgsConstructor
public class PlatformCompanySuspendProcessor extends BaseBizProcessor<BizContext> {

    private final ObjectMapper objectMapper;
    private final CompanyMapper companyMapper;

    @Override
    protected Object doProcess(BizContext context, JsonNode payload) {
        PlatformCompanySuspendRequest request =
                objectMapper.convertValue(payload, PlatformCompanySuspendRequest.class);

        CompanyEntity company = companyMapper.selectByPrimaryKey(request.getCompanyId());
        if (company == null) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "Company not found");
        }

        company.setStatus("SUSPENDED");
        company.setUpdatedAt(new Date());
        companyMapper.updateByPrimaryKey(company);

        PlatformCompanySuspendResponse response = new PlatformCompanySuspendResponse();
        response.setCompanyId(company.getCompanyId());
        response.setStatus(company.getStatus());
        response.setMessage("Company suspended successfully");
        return response;
    }
}