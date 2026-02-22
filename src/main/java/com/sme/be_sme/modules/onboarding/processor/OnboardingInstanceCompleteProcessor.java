package com.sme.be_sme.modules.onboarding.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.identity.api.request.UpdateUserRequest;
import com.sme.be_sme.modules.identity.context.IdentityUpdateUserContext;
import com.sme.be_sme.modules.identity.processor.IdentityUserUpdateProcessor;
import com.sme.be_sme.modules.onboarding.api.request.OnboardingInstanceCompleteRequest;
import com.sme.be_sme.modules.onboarding.api.response.OnboardingInstanceResponse;
import com.sme.be_sme.modules.onboarding.infrastructure.mapper.OnboardingInstanceMapper;
import com.sme.be_sme.modules.onboarding.infrastructure.persistence.entity.OnboardingInstanceEntity;
import com.sme.be_sme.shared.constant.ErrorCodes;
import com.sme.be_sme.shared.exception.AppException;
import com.sme.be_sme.shared.gateway.core.BaseBizProcessor;
import com.sme.be_sme.shared.gateway.core.BizContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Date;

@Component
@RequiredArgsConstructor
public class OnboardingInstanceCompleteProcessor extends BaseBizProcessor<BizContext> {

    private final ObjectMapper objectMapper;
    private final OnboardingInstanceMapper onboardingInstanceMapper;
    private final IdentityUserUpdateProcessor identityUserUpdateProcessor;

    @Override
    protected Object doProcess(BizContext context, JsonNode payload) {
        OnboardingInstanceCompleteRequest request = objectMapper.convertValue(payload, OnboardingInstanceCompleteRequest.class);
        validate(context, request);

        String companyId = context.getTenantId();
        OnboardingInstanceEntity instance = onboardingInstanceMapper.selectByPrimaryKey(request.getInstanceId().trim());
        if (instance == null) {
            throw AppException.of(ErrorCodes.NOT_FOUND, "onboarding instance not found");
        }
        if (!companyId.equals(instance.getCompanyId())) {
            throw AppException.of(ErrorCodes.FORBIDDEN, "instance does not belong to tenant");
        }

        Date now = new Date();
        instance.setStatus("DONE");
        instance.setCompletedAt(now);
        instance.setCompletedBy(context.getOperatorId());
        instance.setUpdatedAt(now);
        instance.setUpdatedBy(context.getOperatorId());
        int updated = onboardingInstanceMapper.updateByPrimaryKey(instance);
        if (updated != 1) {
            throw AppException.of(ErrorCodes.INTERNAL_ERROR, "complete onboarding instance failed");
        }

        if (StringUtils.hasText(instance.getEmployeeId())) {
            IdentityUpdateUserContext identityContext = new IdentityUpdateUserContext();
            identityContext.setTenantId(context.getTenantId());
            identityContext.setOperatorId(context.getOperatorId());
            UpdateUserRequest updateUserRequest = UpdateUserRequest.builder()
                    .userId(instance.getEmployeeId())
                    .companyId(companyId)
                    .status("OFFICIAL")
                    .build();
            identityUserUpdateProcessor.process(identityContext, updateUserRequest);
        }

        OnboardingInstanceResponse response = new OnboardingInstanceResponse();
        response.setInstanceId(instance.getOnboardingId());
        response.setStatus(instance.getStatus());
        return response;
    }

    private static void validate(BizContext context, OnboardingInstanceCompleteRequest request) {
        if (context == null || !StringUtils.hasText(context.getTenantId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "tenantId is required");
        }
        if (request == null || !StringUtils.hasText(request.getInstanceId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "instanceId is required");
        }
    }
}
