package com.sme.be_sme.modules.onboarding.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.onboarding.api.request.OnboardingManagerEvaluationSendRequest;
import com.sme.be_sme.modules.onboarding.api.response.OnboardingManagerEvaluationSendResponse;
import com.sme.be_sme.modules.onboarding.infrastructure.mapper.OnboardingInstanceMapper;
import com.sme.be_sme.modules.onboarding.infrastructure.persistence.entity.OnboardingInstanceEntity;

import com.sme.be_sme.modules.onboarding.service.ManagerOnboardingEvaluationService;
import com.sme.be_sme.modules.survey.service.ManagerEvaluationSendResult;
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
public class OnboardingManagerEvaluationSendProcessor extends BaseBizProcessor<BizContext> {

    private final ObjectMapper objectMapper;
    private final OnboardingInstanceMapper onboardingInstanceMapper;
    private final ManagerOnboardingEvaluationService managerOnboardingEvaluationService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    protected Object doProcess(BizContext context, JsonNode payload) {
        OnboardingManagerEvaluationSendRequest request =
                objectMapper.convertValue(payload, OnboardingManagerEvaluationSendRequest.class);

        if (context == null || !StringUtils.hasText(context.getTenantId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "tenantId is required");
        }

        if (request == null || !StringUtils.hasText(request.getInstanceId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "instanceId is required");
        }

        String companyId = context.getTenantId();

        OnboardingInstanceEntity instance =
                onboardingInstanceMapper.selectByPrimaryKey(request.getInstanceId().trim());

        if (instance == null) {
            throw AppException.of(ErrorCodes.NOT_FOUND, "onboarding instance not found");
        }

        if (!companyId.equals(instance.getCompanyId())) {
            throw AppException.of(ErrorCodes.FORBIDDEN, "instance does not belong to tenant");
        }

        if (!"DONE".equalsIgnoreCase(instance.getStatus())
                && !"COMPLETED".equalsIgnoreCase(instance.getStatus())) {
            throw AppException.of(
                    ErrorCodes.BAD_REQUEST,
                    "manager evaluation can only be sent after onboarding is completed"
            );
        }

        ManagerEvaluationSendResult result =
                managerOnboardingEvaluationService.sendAfterOnboardingCompleted(
                        companyId,
                        context.getOperatorId(),
                        instance,
                        request.getManagerEvaluationTemplateId(),
                        request.getManagerEvaluationDueDays()
                );

        OnboardingManagerEvaluationSendResponse response =
                new OnboardingManagerEvaluationSendResponse();

        response.setInstanceId(instance.getOnboardingId());
        response.setManagerEvaluationStatus(result.getStatus());
        response.setManagerEvaluationSurveyInstanceId(result.getSurveyInstanceId());
        response.setManagerEvaluationMessage(result.getMessage());

        return response;
    }
}