package com.sme.be_sme.modules.platform.processor.company;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.company.infrastructure.mapper.CompanyMapper;
import com.sme.be_sme.modules.company.infrastructure.persistence.entity.CompanyEntity;
import com.sme.be_sme.modules.platform.api.request.PlatformCompanyStatusRequest;
import com.sme.be_sme.modules.platform.api.response.PlatformCompanyStatusResponse;
import com.sme.be_sme.shared.constant.ErrorCodes;
import com.sme.be_sme.shared.exception.AppException;
import com.sme.be_sme.shared.gateway.core.BaseBizProcessor;
import com.sme.be_sme.shared.gateway.core.BizContext;
import java.util.Date;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class PlatformCompanyActivateProcessor extends BaseBizProcessor<BizContext> {

    private final ObjectMapper objectMapper;
    private final CompanyMapper companyMapper;

    @Override
    protected Object doProcess(BizContext context, JsonNode payload) {
        PlatformCompanyStatusRequest request = objectMapper.convertValue(payload, PlatformCompanyStatusRequest.class);

        if (!StringUtils.hasText(request.getCompanyId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "companyId is required");
        }

        CompanyEntity company = companyMapper.selectByPrimaryKey(request.getCompanyId());
        if (company == null) {
            throw AppException.of(ErrorCodes.NOT_FOUND, "Company not found");
        }

        company.setStatus("ACTIVE");
        company.setUpdatedAt(new Date());
        companyMapper.updateByPrimaryKey(company);

        PlatformCompanyStatusResponse response = new PlatformCompanyStatusResponse();
        response.setCompanyId(company.getCompanyId());
        response.setStatus(company.getStatus());
        return response;
    }
}
