package com.sme.be_sme.modules.survey.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.survey.api.request.SurveyTemplateArchiveRequest;
import com.sme.be_sme.modules.survey.api.response.SurveyTemplateArchiveResponse;
import com.sme.be_sme.modules.survey.infrastructure.mapper.SurveyTemplateMapper;
import com.sme.be_sme.modules.survey.infrastructure.persistence.entity.SurveyTemplateEntity;
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
public class SurveyTemplateArchiveProcessor extends BaseBizProcessor<BizContext> {

    private final ObjectMapper objectMapper;
    private final SurveyTemplateMapper surveyTemplateMapper;

    @Override
    protected Object doProcess(BizContext context, JsonNode payload) {
        SurveyTemplateArchiveRequest req =
                objectMapper.convertValue(payload, SurveyTemplateArchiveRequest.class);

        validate(context, req);

        SurveyTemplateEntity existed = surveyTemplateMapper.selectByPrimaryKey(req.getTemplateId());
        if (existed == null || !context.getTenantId().equals(existed.getCompanyId())) {
            throw AppException.of(ErrorCodes.NOT_FOUND, "survey template not found");
        }

        existed.setStatus("ARCHIVED");
        existed.setIsDefault(false);
        existed.setUpdatedAt(new Date());

        int updated = surveyTemplateMapper.updateByPrimaryKey(existed);
        if (updated != 1) {
            throw AppException.of(ErrorCodes.INTERNAL_ERROR, "archive survey template failed");
        }

        SurveyTemplateArchiveResponse res = new SurveyTemplateArchiveResponse();
        res.setTemplateId(existed.getSurveyTemplateId());
        res.setStatus(existed.getStatus());
        return res;
    }

    private static void validate(BizContext context, SurveyTemplateArchiveRequest req) {
        if (context == null || !StringUtils.hasText(context.getTenantId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "tenantId is required");
        }
        if (req == null || !StringUtils.hasText(req.getTemplateId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "templateId is required");
        }
    }
}