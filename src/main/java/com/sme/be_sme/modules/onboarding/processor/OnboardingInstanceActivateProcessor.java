package com.sme.be_sme.modules.onboarding.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.onboarding.api.request.OnboardingInstanceActivateRequest;
import com.sme.be_sme.modules.onboarding.api.request.OnboardingTaskGenerateRequest;
import com.sme.be_sme.modules.onboarding.api.response.OnboardingInstanceResponse;
import com.sme.be_sme.modules.onboarding.facade.OnboardingTaskFacade;
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
public class OnboardingInstanceActivateProcessor extends BaseBizProcessor<BizContext> {

    private final ObjectMapper objectMapper;
    private final OnboardingInstanceMapper onboardingInstanceMapper;
    private final OnboardingTaskFacade onboardingTaskFacade;

    @Override
    protected Object doProcess(BizContext context, JsonNode payload) {
        OnboardingInstanceActivateRequest request = objectMapper.convertValue(payload, OnboardingInstanceActivateRequest.class);
        validate(context, request);

        String companyId = context.getTenantId();
        OnboardingInstanceEntity instance = null;
        if (StringUtils.hasText(request.getRequestNo())) {
            instance = onboardingInstanceMapper.selectByCompanyIdAndRequestNo(companyId, request.getRequestNo().trim());
            if (instance != null && "ACTIVE".equalsIgnoreCase(instance.getStatus())) {
                OnboardingInstanceResponse response = new OnboardingInstanceResponse();
                response.setInstanceId(instance.getOnboardingId());
                response.setStatus(instance.getStatus());
                return response;
            }
        }
        if (instance == null) {
            if (!StringUtils.hasText(request.getInstanceId())) {
                throw AppException.of(ErrorCodes.BAD_REQUEST, "instanceId is required when requestNo is not provided or does not match");
            }
            instance = onboardingInstanceMapper.selectByPrimaryKey(request.getInstanceId().trim());
        }
        if (instance == null) {
            throw AppException.of(ErrorCodes.NOT_FOUND, "onboarding instance not found");
        }
        if (!companyId.equals(instance.getCompanyId())) {
            throw AppException.of(ErrorCodes.FORBIDDEN, "instance does not belong to tenant");
        }

        Date now = new Date();
        boolean wasInactive = !"ACTIVE".equalsIgnoreCase(instance.getStatus());
        if (wasInactive) {
            instance.setStatus("ACTIVE");
        }
        if (instance.getStartDate() == null) {
            instance.setStartDate(now);
        }
        instance.setUpdatedAt(now);
        if (StringUtils.hasText(request.getRequestNo())) {
            instance.setRequestNo(request.getRequestNo().trim());
        }

        int updated = onboardingInstanceMapper.updateByPrimaryKey(instance);
        if (updated != 1) {
            throw AppException.of(ErrorCodes.INTERNAL_ERROR, "activate onboarding instance failed");
        }

        if (wasInactive) {
            OnboardingTaskGenerateRequest genReq = new OnboardingTaskGenerateRequest();
            genReq.setInstanceId(instance.getOnboardingId());
            onboardingTaskFacade.generateTasksFromTemplate(genReq);
        }

        OnboardingInstanceResponse response = new OnboardingInstanceResponse();
        response.setInstanceId(instance.getOnboardingId());
        response.setStatus(instance.getStatus());
        return response;
    }

    private static void validate(BizContext context, OnboardingInstanceActivateRequest request) {
        if (context == null || !StringUtils.hasText(context.getTenantId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "tenantId is required");
        }
        if (request == null) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "payload is required");
        }
        if (!StringUtils.hasText(request.getInstanceId()) && !StringUtils.hasText(request.getRequestNo())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "instanceId or requestNo is required");
        }
    }
}
