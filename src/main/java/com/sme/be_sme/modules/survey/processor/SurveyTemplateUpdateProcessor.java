package com.sme.be_sme.modules.survey.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.survey.api.request.SurveyTemplateUpdateRequest;
import com.sme.be_sme.modules.survey.api.response.SurveyTemplateResponse;
import com.sme.be_sme.modules.survey.infrastructure.mapper.SurveyInstanceMapperExt;
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
public class SurveyTemplateUpdateProcessor extends BaseBizProcessor<BizContext> {

    private final ObjectMapper objectMapper;
    private final SurveyTemplateMapper surveyTemplateMapper;
    private final SurveyInstanceMapperExt surveyInstanceMapperExt;

    @Override
    protected Object doProcess(BizContext context, JsonNode payload) {

        if (context == null || !StringUtils.hasText(context.getTenantId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "tenantId is required");
        }

        SurveyTemplateUpdateRequest request =
                objectMapper.convertValue(payload, SurveyTemplateUpdateRequest.class);

        if (request == null || !StringUtils.hasText(request.getTemplateId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "templateId is required");
        }

        SurveyTemplateEntity entity =
                surveyTemplateMapper.selectByPrimaryKey(request.getTemplateId().trim());

        if (entity == null || !context.getTenantId().equals(entity.getCompanyId())) {
            throw AppException.of(ErrorCodes.NOT_FOUND, "survey template not found");
        }

        if ("ARCHIVED".equalsIgnoreCase(entity.getStatus())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "archived template cannot be updated");
        }

        int instanceCount = surveyInstanceMapperExt.countByCompanyId(
                context.getTenantId(),
                entity.getSurveyTemplateId(),
                null,
                null,
                null,
                null
        );
        if (instanceCount > 0) {
            throw AppException.of(
                    ErrorCodes.BAD_REQUEST,
                    "template already has instances, update is not allowed"
            );
        }

        if (request.getVersion() != null
                && entity.getVersion() != null
                && !request.getVersion().equals(entity.getVersion())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "version mismatch");
        }

        String nextStage = StringUtils.hasText(request.getStage())
                ? request.getStage().trim()
                : entity.getStage();

        String nextTargetRole = StringUtils.hasText(request.getTargetRole())
                ? request.getTargetRole().trim()
                : entity.getTargetRole();

        String nextStatus = StringUtils.hasText(request.getStatus())
                ? request.getStatus().trim()
                : entity.getStatus();

        Boolean nextIsDefault = request.getIsDefault() != null
                ? request.getIsDefault()
                : entity.getIsDefault();

        boolean forceReplaceDefault = Boolean.TRUE.equals(request.getForceReplaceDefault());

        validateUpdateValues(nextStage, nextStatus, nextTargetRole);

        if ("CUSTOM".equals(nextStage) && Boolean.TRUE.equals(nextIsDefault)) {
            throw AppException.of(
                    ErrorCodes.BAD_REQUEST,
                    "CUSTOM_STAGE_CANNOT_BE_DEFAULT"
            );
        }

        if (Boolean.TRUE.equals(nextIsDefault) && !"ACTIVE".equals(nextStatus)) {
            throw AppException.of(
                    ErrorCodes.BAD_REQUEST,
                    "ONLY_ACTIVE_TEMPLATE_CAN_BE_DEFAULT"
            );
        }

        if (Boolean.TRUE.equals(nextIsDefault)) {
            SurveyTemplateEntity oldDefault =
                    surveyTemplateMapper.findActiveDefaultByCompanyStageAndTargetRoleExcludingTemplateId(
                            context.getTenantId(),
                            nextStage,
                            nextTargetRole,
                            entity.getSurveyTemplateId()
                    );

            if (oldDefault != null && !forceReplaceDefault) {
                throw AppException.of(
                        ErrorCodes.BAD_REQUEST,
                        "DEFAULT_TEMPLATE_ALREADY_EXISTS"
                );
            }

            if (oldDefault != null) {
                surveyTemplateMapper.clearDefaultByCompanyStageAndTargetRoleExcludingTemplateId(
                        context.getTenantId(),
                        nextStage,
                        nextTargetRole,
                        entity.getSurveyTemplateId()
                );
            }
        }

        if (StringUtils.hasText(request.getName())) {
            String nextName = request.getName().trim();
            if (nextName.length() > 255) {
                throw AppException.of(ErrorCodes.BAD_REQUEST, "name is too long");
            }
            entity.setName(nextName);
        }

        if (request.getDescription() != null) {
            if (request.getDescription().length() > 5000) {
                throw AppException.of(ErrorCodes.BAD_REQUEST, "description is too long");
            }
            entity.setDescription(request.getDescription());
        }

        if (StringUtils.hasText(request.getStage())) {
            entity.setStage(nextStage);
        }

        if (request.getManagerOnly() != null) {
            entity.setManagerOnly(request.getManagerOnly());
        }

        if (StringUtils.hasText(request.getStatus())) {
            entity.setStatus(nextStatus);
        }

        if (request.getIsDefault() != null) {
            entity.setIsDefault(request.getIsDefault());
        }

        if (StringUtils.hasText(request.getTargetRole())) {
            entity.setTargetRole(nextTargetRole);
        }

        if (entity.getVersion() == null) {
            entity.setVersion(1);
        } else {
            entity.setVersion(entity.getVersion() + 1);
        }

        entity.setUpdatedAt(new Date());

        int updated = surveyTemplateMapper.updateByPrimaryKey(entity);
        if (updated != 1) {
            throw AppException.of(ErrorCodes.INTERNAL_ERROR, "update survey template failed");
        }

        SurveyTemplateResponse res = new SurveyTemplateResponse();
        res.setTemplateId(entity.getSurveyTemplateId());
        res.setName(entity.getName());
        res.setStatus(entity.getStatus());
        res.setDescription(entity.getDescription());
        res.setStage(entity.getStage());
        res.setManagerOnly(entity.getManagerOnly());
        res.setVersion(entity.getVersion());
        res.setIsDefault(entity.getIsDefault());
        res.setTargetRole(entity.getTargetRole());
        return res;
    }

    private static void validateUpdateValues(
            String stage,
            String status,
            String targetRole
    ) {
        if (StringUtils.hasText(stage)) {
            String value = stage.trim();
            if (!"D7".equals(value)
                    && !"D30".equals(value)
                    && !"D60".equals(value)
                    && !"CUSTOM".equals(value)) {
                throw AppException.of(ErrorCodes.BAD_REQUEST, "invalid stage");
            }
        }

        if (StringUtils.hasText(status)) {
            String value = status.trim();
            if (!"DRAFT".equals(value)
                    && !"ACTIVE".equals(value)
                    && !"ARCHIVED".equals(value)
                    && !"DISABLED".equals(value)) {
                throw AppException.of(ErrorCodes.BAD_REQUEST, "invalid status");
            }
        }

        if (StringUtils.hasText(targetRole)) {
            String value = targetRole.trim();
            if (!"EMPLOYEE".equals(value) && !"MANAGER".equals(value)) {
                throw AppException.of(ErrorCodes.BAD_REQUEST, "invalid targetRole");
            }
        }
    }
}