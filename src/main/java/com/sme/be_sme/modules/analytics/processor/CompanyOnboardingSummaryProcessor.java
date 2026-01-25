package com.sme.be_sme.modules.analytics.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.analytics.api.request.CompanyOnboardingSummaryRequest;
import com.sme.be_sme.modules.analytics.api.response.CompanyOnboardingSummaryResponse;
import com.sme.be_sme.shared.gateway.core.BaseBizProcessor;
import com.sme.be_sme.shared.gateway.core.BizContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CompanyOnboardingSummaryProcessor extends BaseBizProcessor<BizContext> {

    private final ObjectMapper objectMapper;

    @Override
    protected Object doProcess(BizContext context, JsonNode payload) {
        CompanyOnboardingSummaryRequest request = objectMapper.convertValue(payload, CompanyOnboardingSummaryRequest.class);
        CompanyOnboardingSummaryResponse response = new CompanyOnboardingSummaryResponse();
        response.setCompanyId(request.getCompanyId());
        response.setTotalEmployees(0);
        response.setCompletedCount(0);
        return response;
    }
}
