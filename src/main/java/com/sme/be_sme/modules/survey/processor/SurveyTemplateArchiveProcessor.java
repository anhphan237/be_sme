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
        if (context == null || !StringUtils.hasText(context.getTenantId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "tenantId is required");
        }

        SurveyTemplateArchiveRequest request =
                objectMapper.convertValue(payload, SurveyTemplateArchiveRequest.class);

        if (request == null || !StringUtils.hasText(request.getTemplateId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "templateId is required");
        }

        SurveyTemplateEntity template =
                surveyTemplateMapper.selectByPrimaryKey(request.getTemplateId());

        if (template == null || !context.getTenantId().equals(template.getCompanyId())) {
            throw AppException.of(ErrorCodes.NOT_FOUND, "survey template not found");
        }

        template.setStatus("ARCHIVED");
        template.setUpdatedAt(new Date());

        int updated = surveyTemplateMapper.updateByPrimaryKey(template);
        if (updated != 1) {
            throw AppException.of(ErrorCodes.INTERNAL_ERROR, "archive survey template failed");
        }

        SurveyTemplateArchiveResponse res = new SurveyTemplateArchiveResponse();
        res.setTemplateId(template.getSurveyTemplateId());
        res.setStatus(template.getStatus());
        return res;
    }
}
