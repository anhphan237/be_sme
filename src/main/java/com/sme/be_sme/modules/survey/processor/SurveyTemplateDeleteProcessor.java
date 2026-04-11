package com.sme.be_sme.modules.survey.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.survey.api.request.SurveyTemplateDeleteRequest;
import com.sme.be_sme.modules.survey.api.response.SurveyTemplateDeleteResponse;
import com.sme.be_sme.modules.survey.infrastructure.mapper.SurveyTemplateMapper;
import com.sme.be_sme.modules.survey.infrastructure.mapper.SurveyTemplateMapperExt;
import com.sme.be_sme.modules.survey.infrastructure.mapper.SurveyQuestionMapperExt;
import com.sme.be_sme.modules.survey.infrastructure.persistence.entity.SurveyTemplateEntity;
import com.sme.be_sme.shared.constant.ErrorCodes;
import com.sme.be_sme.shared.exception.AppException;
import com.sme.be_sme.shared.gateway.core.BaseBizProcessor;
import com.sme.be_sme.shared.gateway.core.BizContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class SurveyTemplateDeleteProcessor extends BaseBizProcessor<BizContext> {

    private final ObjectMapper objectMapper;
    private final SurveyTemplateMapper surveyTemplateMapper;
    private final SurveyTemplateMapperExt surveyTemplateMapperExt;
    private final SurveyQuestionMapperExt surveyQuestionMapperExt;

    @Override
    @Transactional
    protected Object doProcess(BizContext context, JsonNode payload) {
        SurveyTemplateDeleteRequest req =
                objectMapper.convertValue(payload, SurveyTemplateDeleteRequest.class);

        validate(context, req);

        SurveyTemplateEntity existed = surveyTemplateMapper.selectByPrimaryKey(req.getTemplateId());

        if (existed == null) {
            throw AppException.of(ErrorCodes.NOT_FOUND, "survey template not found");
        }

        if (!context.getTenantId().equals(existed.getCompanyId())) {
            throw AppException.of(ErrorCodes.NOT_FOUND, "survey template not found");
        }

        boolean hasInstances = surveyTemplateMapperExt.existsAnyInstanceByTemplateId(
                req.getTemplateId(),
                context.getTenantId()
        );

        if (hasInstances) {
            throw AppException.of(
                    ErrorCodes.BAD_REQUEST,
                    "template already used, only archive is allowed"
            );
        }

        surveyQuestionMapperExt.deleteByTemplateId(
                req.getTemplateId(),
                context.getTenantId()
        );

        int deleted = surveyTemplateMapperExt.deleteByIdAndCompanyId(
                req.getTemplateId(),
                context.getTenantId()
        );

        if (deleted != 1) {
            throw AppException.of(ErrorCodes.INTERNAL_ERROR, "delete survey template failed");
        }

        SurveyTemplateDeleteResponse res = new SurveyTemplateDeleteResponse();
        res.setTemplateId(req.getTemplateId());
        res.setDeleted(true);
        return res;
    }

    private static void validate(BizContext context, SurveyTemplateDeleteRequest req) {
        if (context == null || !StringUtils.hasText(context.getTenantId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "tenantId is required");
        }
        if (req == null || !StringUtils.hasText(req.getTemplateId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "templateId is required");
        }
    }
}