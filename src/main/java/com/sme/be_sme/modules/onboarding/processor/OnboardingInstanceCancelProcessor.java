package com.sme.be_sme.modules.onboarding.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.onboarding.api.request.OnboardingInstanceCancelRequest;
import com.sme.be_sme.modules.onboarding.api.response.OnboardingInstanceResponse;
import com.sme.be_sme.modules.onboarding.infrastructure.mapper.OnboardingInstanceMapper;
import com.sme.be_sme.modules.onboarding.infrastructure.persistence.entity.OnboardingInstanceEntity;
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
public class OnboardingInstanceCancelProcessor extends BaseBizProcessor<BizContext> {

    private final ObjectMapper objectMapper;
    private final OnboardingInstanceMapper onboardingInstanceMapper;

    @Override
    protected Object doProcess(BizContext context, JsonNode payload) {
        OnboardingInstanceCancelRequest request = objectMapper.convertValue(payload, OnboardingInstanceCancelRequest.class);
        validate(context, request);

        OnboardingInstanceEntity instance = onboardingInstanceMapper.selectByPrimaryKey(request.getInstanceId().trim());
        if (instance == null) {
            throw AppException.of(ErrorCodes.NOT_FOUND, "onboarding instance not found");
        }
        if (!context.getTenantId().equals(instance.getCompanyId())) {
            throw AppException.of(ErrorCodes.FORBIDDEN, "instance does not belong to tenant");
        }

        Date now = new Date();
        if (!"CANCELLED".equalsIgnoreCase(instance.getStatus())) {
            instance.setStatus("CANCELLED");
        }
        instance.setUpdatedAt(now);
        instance.setUpdatedBy(context.getOperatorId());

        int updated = onboardingInstanceMapper.updateByPrimaryKey(instance);
        if (updated != 1) {
            throw AppException.of(ErrorCodes.INTERNAL_ERROR, "cancel onboarding instance failed");
        }

        OnboardingInstanceResponse response = new OnboardingInstanceResponse();
        response.setInstanceId(instance.getOnboardingId());
        response.setStatus(instance.getStatus());
        return response;
    }

    private static void validate(BizContext context, OnboardingInstanceCancelRequest request) {
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
