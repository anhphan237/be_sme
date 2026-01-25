package com.sme.be_sme.modules.onboarding.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.onboarding.api.request.OnboardingInstanceCreateRequest;
import com.sme.be_sme.modules.onboarding.api.response.OnboardingInstanceResponse;
import com.sme.be_sme.modules.onboarding.infrastructure.mapper.OnboardingInstanceMapper;
import com.sme.be_sme.modules.onboarding.infrastructure.mapper.OnboardingTemplateMapper;
import com.sme.be_sme.modules.onboarding.infrastructure.persistence.entity.OnboardingInstanceEntity;
import com.sme.be_sme.modules.onboarding.infrastructure.persistence.entity.OnboardingTemplateEntity;
import com.sme.be_sme.shared.constant.ErrorCodes;
import com.sme.be_sme.shared.exception.AppException;
import com.sme.be_sme.shared.gateway.core.BaseBizProcessor;
import com.sme.be_sme.shared.gateway.core.BizContext;
import com.sme.be_sme.shared.util.UuidGenerator;
import java.util.Date;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class OnboardingInstanceCreateProcessor extends BaseBizProcessor<BizContext> {

    private final ObjectMapper objectMapper;
    private final OnboardingInstanceMapper onboardingInstanceMapper;
    private final OnboardingTemplateMapper onboardingTemplateMapper;

    @Override
    protected Object doProcess(BizContext context, JsonNode payload) {
        OnboardingInstanceCreateRequest request = objectMapper.convertValue(payload, OnboardingInstanceCreateRequest.class);
        if (!StringUtils.hasText(request.getTemplateId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "templateId is required");
        }
        if (!StringUtils.hasText(request.getEmployeeUserId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "employeeUserId is required");
        }

        OnboardingTemplateEntity template = onboardingTemplateMapper.selectByPrimaryKey(request.getTemplateId());
        if (template == null || !context.getTenantId().equals(template.getCompanyId())) {
            throw AppException.of(ErrorCodes.NOT_FOUND, "onboarding template not found");
        }

        Date now = new Date();
        String instanceId = UuidGenerator.generate();

        OnboardingInstanceEntity entity = new OnboardingInstanceEntity();
        entity.setOnboardingId(instanceId);
        entity.setCompanyId(context.getTenantId());
        entity.setEmployeeId(request.getEmployeeUserId().trim());
        entity.setOnboardingTemplateId(template.getOnboardingTemplateId());
        entity.setStatus("DRAFT");
        entity.setStartDate(now);
        entity.setCreatedBy("system");
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);

        int inserted = onboardingInstanceMapper.insert(entity);
        if (inserted != 1) {
            throw AppException.of(ErrorCodes.INTERNAL_ERROR, "create onboarding instance failed");
        }

        OnboardingInstanceResponse response = new OnboardingInstanceResponse();
        response.setInstanceId(instanceId);
        response.setStatus(entity.getStatus());
        return response;
    }
}
