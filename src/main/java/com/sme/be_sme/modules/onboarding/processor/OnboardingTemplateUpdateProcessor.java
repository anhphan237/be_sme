package com.sme.be_sme.modules.onboarding.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.onboarding.api.request.OnboardingTemplateUpdateRequest;
import com.sme.be_sme.modules.onboarding.api.response.OnboardingTemplateResponse;
import com.sme.be_sme.modules.onboarding.infrastructure.mapper.ChecklistTemplateMapper;
import com.sme.be_sme.modules.onboarding.infrastructure.mapper.OnboardingTemplateMapper;
import com.sme.be_sme.modules.onboarding.infrastructure.mapper.OnboardingTemplateMapperExt;
import com.sme.be_sme.modules.onboarding.infrastructure.mapper.TaskTemplateMapper;
import com.sme.be_sme.modules.onboarding.infrastructure.persistence.entity.OnboardingTemplateEntity;
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
public class OnboardingTemplateUpdateProcessor extends BaseBizProcessor<BizContext> {

    private final ObjectMapper objectMapper;
    private final OnboardingTemplateMapper onboardingTemplateMapper;
    private final OnboardingTemplateMapperExt onboardingTemplateMapperExt;
    private final ChecklistTemplateMapper checklistTemplateMapper;
    private final TaskTemplateMapper taskTemplateMapper;

    @Override
    protected Object doProcess(BizContext context, JsonNode payload) {
        OnboardingTemplateUpdateRequest request = objectMapper.convertValue(payload, OnboardingTemplateUpdateRequest.class);
        validate(context, request);

        String companyId = context.getTenantId();
        OnboardingTemplateEntity entity = onboardingTemplateMapperExt.selectTemplateByIdAndCompany(
                request.getTemplateId(), companyId);
        if (entity == null) {
            throw AppException.of(ErrorCodes.NOT_FOUND, "onboarding template not found");
        }

        if (request.getName() != null) {
            entity.setName(request.getName().trim());
        }
        if (request.getDescription() != null) {
            entity.setDescription(request.getDescription());
        }
        if (request.getStatus() != null) {
            entity.setStatus(request.getStatus().trim());
        }
        entity.setUpdatedAt(new Date());

        int updated = onboardingTemplateMapper.updateByPrimaryKey(entity);
        if (updated != 1) {
            throw AppException.of(ErrorCodes.INTERNAL_ERROR, "update onboarding template failed");
        }

        if (request.getStatus() != null) {
            Date now = entity.getUpdatedAt();
            String newStatus = entity.getStatus();
            checklistTemplateMapper.updateStatusByOnboardingTemplateId(
                    entity.getOnboardingTemplateId(), newStatus, now);
            taskTemplateMapper.updateStatusByOnboardingTemplateId(
                    entity.getOnboardingTemplateId(), newStatus, now);
        }

        OnboardingTemplateResponse response = new OnboardingTemplateResponse();
        response.setTemplateId(entity.getOnboardingTemplateId());
        response.setName(entity.getName());
        response.setStatus(entity.getStatus());
        return response;
    }

    private static void validate(BizContext context, OnboardingTemplateUpdateRequest request) {
        if (context == null || !StringUtils.hasText(context.getTenantId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "tenantId is required");
        }
        if (request == null) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "payload is required");
        }
        if (!StringUtils.hasText(request.getTemplateId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "templateId is required");
        }
        if (request.getName() != null && !StringUtils.hasText(request.getName())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "name is required");
        }
        if (request.getStatus() != null && !StringUtils.hasText(request.getStatus())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "status is required");
        }
    }
}
