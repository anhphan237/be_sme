package com.sme.be_sme.modules.survey.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.survey.api.request.SurveyResponseListRequest;
import com.sme.be_sme.modules.survey.api.response.SurveyResponseItem;
import com.sme.be_sme.modules.survey.api.response.SurveyResponseListResponse;
import com.sme.be_sme.modules.survey.infrastructure.mapper.SurveyInstanceMapper;
import com.sme.be_sme.shared.constant.ErrorCodes;
import com.sme.be_sme.shared.exception.AppException;
import com.sme.be_sme.shared.gateway.core.BaseBizProcessor;
import com.sme.be_sme.shared.gateway.core.BizContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class SurveyResponseListProcessor extends BaseBizProcessor<BizContext> {

    private final ObjectMapper objectMapper;
    private final SurveyInstanceMapper surveyInstanceMapper;

    @Override
    protected Object doProcess(BizContext context, JsonNode payload) {
        SurveyResponseListRequest request =
                objectMapper.convertValue(payload, SurveyResponseListRequest.class);

        if (context == null || context.getTenantId() == null) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "tenantId is required");
        }

        List<SurveyResponseItem> items =
                surveyInstanceMapper.selectResponses(
                        context.getTenantId(),
                        request.getTemplateId(),
                        request.getOnboardingId(),
                        request.getStatus()
                );

        SurveyResponseListResponse res = new SurveyResponseListResponse();
        res.setItems(items);
        return res;
    }
}

