package com.sme.be_sme.modules.survey.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.survey.api.request.SurveyManagerEvaluationTemplateCreateRequest;
import com.sme.be_sme.modules.survey.api.response.SurveyTemplateResponse;
import com.sme.be_sme.modules.survey.infrastructure.mapper.SurveyTemplateMapper;
import com.sme.be_sme.modules.survey.infrastructure.mapper.SurveyTemplateMapperExt;
import com.sme.be_sme.modules.survey.infrastructure.persistence.entity.SurveyTemplateEntity;
import com.sme.be_sme.shared.constant.ErrorCodes;
import com.sme.be_sme.shared.exception.AppException;
import com.sme.be_sme.shared.gateway.core.BaseBizProcessor;
import com.sme.be_sme.shared.gateway.core.BizContext;
import com.sme.be_sme.shared.util.UuidGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Date;

@Component
@RequiredArgsConstructor
public class SurveyManagerEvaluationTemplateCreateProcessor extends BaseBizProcessor<BizContext> {

    private static final String STAGE_COMPLETED = "COMPLETED";
    private static final String TARGET_ROLE_MANAGER = "MANAGER";
    private static final String STATUS_ACTIVE = "ACTIVE";

    private final ObjectMapper objectMapper;
    private final SurveyTemplateMapper surveyTemplateMapper;
    private final SurveyTemplateMapperExt surveyTemplateMapperExt;

    @Override
    @Transactional(rollbackFor = Exception.class)
    protected Object doProcess(BizContext context, JsonNode payload) {
        SurveyManagerEvaluationTemplateCreateRequest request =
                objectMapper.convertValue(payload, SurveyManagerEvaluationTemplateCreateRequest.class);

        validate(context, request);

        String companyId = context.getTenantId();
        Boolean isDefault = Boolean.TRUE.equals(request.getIsDefault());

        if (isDefault) {
            SurveyTemplateEntity existedDefault =
                    surveyTemplateMapperExt.findActiveDefaultByCompanyStageAndTargetRole(
                            companyId,
                            STAGE_COMPLETED,
                            TARGET_ROLE_MANAGER
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
                    ""
            );
        }

        Date now = new Date();
        String templateId = UuidGenerator.generate();

        SurveyTemplateEntity entity = new SurveyTemplateEntity();
        entity.setSurveyTemplateId(templateId);
        entity.setCompanyId(companyId);
        entity.setName(request.getName().trim());
        entity.setDescription(request.getDescription());
        entity.setPurpose("MANAGER_EVALUATION");
        entity.setStage(STAGE_COMPLETED);
        entity.setTargetRole(TARGET_ROLE_MANAGER);
        entity.setManagerOnly(Boolean.TRUE);
        entity.setStatus(STATUS_ACTIVE);
        entity.setVersion(1);
        entity.setIsDefault(isDefault);
        entity.setCreatedBy(StringUtils.hasText(context.getOperatorId()) ? context.getOperatorId() : "system");
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);

        int inserted = surveyTemplateMapper.insert(entity);
        if (inserted != 1) {
            throw AppException.of(ErrorCodes.INTERNAL_ERROR, "create manager evaluation survey template failed");
        }

        return toResponse(entity);
    }

    private static void validate(BizContext context, SurveyManagerEvaluationTemplateCreateRequest request) {
        if (context == null || !StringUtils.hasText(context.getTenantId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "tenantId is required");
        }

        if (request == null) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "payload is required");
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