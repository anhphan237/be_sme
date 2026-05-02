package com.sme.be_sme.modules.survey.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.survey.api.request.SurveyManagerEvaluationTemplateUpdateRequest;
import com.sme.be_sme.modules.survey.api.response.SurveyTemplateResponse;
import com.sme.be_sme.modules.survey.infrastructure.mapper.SurveyTemplateMapper;
import com.sme.be_sme.modules.survey.infrastructure.mapper.SurveyTemplateMapperExt;
import com.sme.be_sme.modules.survey.infrastructure.persistence.entity.SurveyTemplateEntity;
import com.sme.be_sme.shared.constant.ErrorCodes;
import com.sme.be_sme.shared.exception.AppException;
import com.sme.be_sme.shared.gateway.core.BaseBizProcessor;
import com.sme.be_sme.shared.gateway.core.BizContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Date;

@Component
@RequiredArgsConstructor
public class SurveyManagerEvaluationTemplateUpdateProcessor extends BaseBizProcessor<BizContext> {

    private static final String STAGE_COMPLETED = "COMPLETED";
    private static final String TARGET_ROLE_MANAGER = "MANAGER";
    private static final String STATUS_ACTIVE = "ACTIVE";

    private final ObjectMapper objectMapper;
    private final SurveyTemplateMapper surveyTemplateMapper;
    private final SurveyTemplateMapperExt surveyTemplateMapperExt;

    @Override
    @Transactional(rollbackFor = Exception.class)
    protected Object doProcess(BizContext context, JsonNode payload) {
        SurveyManagerEvaluationTemplateUpdateRequest request =
                objectMapper.convertValue(payload, SurveyManagerEvaluationTemplateUpdateRequest.class);

        validate(context, request);

        String companyId = context.getTenantId();
        String templateId = request.getTemplateId().trim();

        SurveyTemplateEntity entity = surveyTemplateMapper.selectByPrimaryKey(templateId);

        if (entity == null) {
            throw AppException.of(ErrorCodes.NOT_FOUND, "survey template not found");
        }

        if (!companyId.equals(entity.getCompanyId())) {
            throw AppException.of(ErrorCodes.FORBIDDEN, "survey template does not belong to tenant");
        }

        if (!STAGE_COMPLETED.equalsIgnoreCase(entity.getStage())
                || !TARGET_ROLE_MANAGER.equalsIgnoreCase(entity.getTargetRole())) {
            throw AppException.of(
                    ErrorCodes.BAD_REQUEST,
                    "this API only supports manager evaluation templates"
            );
        }

        Boolean isDefault = Boolean.TRUE.equals(request.getIsDefault());

        if (isDefault) {
            SurveyTemplateEntity existedDefault =
                    surveyTemplateMapperExt.findActiveDefaultByCompanyStageAndTargetRoleExcludingTemplateId(
                            companyId,
                            STAGE_COMPLETED,
                            TARGET_ROLE_MANAGER,
                            templateId
                    );

            if (existedDefault != null && !Boolean.TRUE.equals(request.getForceReplaceDefault())) {
                throw AppException.of(
                        ErrorCodes.BAD_REQUEST,
                        "DEFAULT_TEMPLATE_ALREADY_EXISTS"
                );
            }

            surveyTemplateMapperExt.clearDefaultByCompanyStageAndTargetRoleExcludingTemplateId(
                    companyId,
                    STAGE_COMPLETED,
                    TARGET_ROLE_MANAGER,
                    templateId
            );
        }

        entity.setName(request.getName().trim());
        entity.setDescription(request.getDescription());
        entity.setPurpose("MANAGER_EVALUATION");
        entity.setStage(STAGE_COMPLETED);
        entity.setTargetRole(TARGET_ROLE_MANAGER);
        entity.setManagerOnly(Boolean.TRUE);
        entity.setStatus(STATUS_ACTIVE);
        entity.setIsDefault(isDefault);
        entity.setUpdatedAt(new Date());

        int updated = surveyTemplateMapper.updateByPrimaryKey(entity);
        if (updated != 1) {
            throw AppException.of(ErrorCodes.INTERNAL_ERROR, "update manager evaluation survey template failed");
        }

        return toResponse(entity);
    }

    private static void validate(BizContext context, SurveyManagerEvaluationTemplateUpdateRequest request) {
        if (context == null || !StringUtils.hasText(context.getTenantId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "tenantId is required");
        }

        if (request == null) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "payload is required");
        }

        if (!StringUtils.hasText(request.getTemplateId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "templateId is required");
        }

        if (!StringUtils.hasText(request.getName())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "name is required");
        }

        if (request.getName().trim().length() > 255) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "name is too long");
        }

        if (request.getDescription() != null && request.getDescription().length() > 5000) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "description is too long");
        }
    }

    private static SurveyTemplateResponse toResponse(SurveyTemplateEntity entity) {
        SurveyTemplateResponse response = new SurveyTemplateResponse();
        response.setTemplateId(entity.getSurveyTemplateId());
        response.setName(entity.getName());
        response.setDescription(entity.getDescription());
        response.setStage(entity.getStage());
        response.setManagerOnly(entity.getManagerOnly());
        response.setStatus(entity.getStatus());
        response.setVersion(entity.getVersion());
        response.setCreatedBy(entity.getCreatedBy());
        response.setCreatedAt(entity.getCreatedAt());
        response.setTargetRole(entity.getTargetRole());
        response.setIsDefault(entity.getIsDefault());
        return response;
    }
}
