package com.sme.be_sme.modules.onboarding.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.onboarding.api.request.OnboardingInstanceGetRequest;
import com.sme.be_sme.modules.onboarding.api.response.OnboardingInstanceDetailResponse;
import com.sme.be_sme.modules.onboarding.infrastructure.mapper.OnboardingInstanceMapper;
import com.sme.be_sme.modules.onboarding.infrastructure.persistence.entity.OnboardingInstanceEntity;
import com.sme.be_sme.shared.constant.ErrorCodes;
import com.sme.be_sme.shared.exception.AppException;
import com.sme.be_sme.shared.gateway.core.BaseBizProcessor;
import com.sme.be_sme.shared.gateway.core.BizContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class OnboardingInstanceGetProcessor extends BaseBizProcessor<BizContext> {

    private final ObjectMapper objectMapper;
    private final OnboardingInstanceMapper onboardingInstanceMapper;

    @Override
    protected Object doProcess(BizContext context, JsonNode payload) {
        OnboardingInstanceGetRequest request = objectMapper.convertValue(payload, OnboardingInstanceGetRequest.class);
        validate(context, request);

        OnboardingInstanceEntity instance = onboardingInstanceMapper.selectByPrimaryKey(request.getInstanceId().trim());
        if (instance == null) {
            throw AppException.of(ErrorCodes.NOT_FOUND, "onboarding instance not found");
        }
        if (!context.getTenantId().equals(instance.getCompanyId())) {
            throw AppException.of(ErrorCodes.FORBIDDEN, "instance does not belong to tenant");
        }

        OnboardingInstanceDetailResponse response = new OnboardingInstanceDetailResponse();
        response.setInstanceId(instance.getOnboardingId());
        response.setEmployeeId(instance.getEmployeeId());
        response.setTemplateId(instance.getOnboardingTemplateId());
        response.setStatus(instance.getStatus());
        response.setStartDate(instance.getStartDate());
        response.setCompletedAt(instance.getCompletedAt());
        return response;
    }

    private static void validate(BizContext context, OnboardingInstanceGetRequest request) {
        if (context == null || !StringUtils.hasText(context.getTenantId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "tenantId is required");
        }
        if (request == null) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "payload is required");
        }
        if (!StringUtils.hasText(request.getInstanceId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "instanceId is required");
        }
    }
}
