package com.sme.be_sme.modules.onboarding.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.employee.infrastructure.mapper.EmployeeProfileMapper;
import com.sme.be_sme.modules.employee.infrastructure.persistence.entity.EmployeeProfileEntity;
import com.sme.be_sme.modules.identity.api.request.UpdateUserRequest;
import com.sme.be_sme.modules.identity.context.IdentityUpdateUserContext;
import com.sme.be_sme.modules.identity.processor.IdentityUserUpdateProcessor;
import com.sme.be_sme.modules.onboarding.api.request.OnboardingInstanceCompleteRequest;
import com.sme.be_sme.modules.onboarding.api.response.OnboardingInstanceResponse;
import com.sme.be_sme.modules.onboarding.infrastructure.mapper.OnboardingInstanceMapper;
import com.sme.be_sme.modules.onboarding.infrastructure.mapper.TaskInstanceMapper;
import com.sme.be_sme.modules.onboarding.infrastructure.persistence.entity.OnboardingInstanceEntity;
import com.sme.be_sme.modules.onboarding.infrastructure.persistence.entity.TaskInstanceEntity;

import com.sme.be_sme.modules.onboarding.service.ManagerOnboardingEvaluationService;
import com.sme.be_sme.modules.onboarding.service.OnboardingInstanceProgressService;
import com.sme.be_sme.modules.survey.service.ManagerEvaluationSendResult;
import com.sme.be_sme.shared.constant.ErrorCodes;
import com.sme.be_sme.shared.exception.AppException;
import com.sme.be_sme.shared.gateway.core.BaseBizProcessor;
import com.sme.be_sme.shared.gateway.core.BizContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.List;

@Component
@RequiredArgsConstructor
public class OnboardingInstanceCompleteProcessor extends BaseBizProcessor<BizContext> {

    private static final String MODE_SEND_NOW = "SEND_NOW";
    private static final String MODE_SEND_LATER = "SEND_LATER";

    private final ObjectMapper objectMapper;
    private final OnboardingInstanceMapper onboardingInstanceMapper;
    private final TaskInstanceMapper taskInstanceMapper;
    private final EmployeeProfileMapper employeeProfileMapper;
    private final IdentityUserUpdateProcessor identityUserUpdateProcessor;
    private final ManagerOnboardingEvaluationService managerOnboardingEvaluationService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    protected Object doProcess(BizContext context, JsonNode payload) {
        OnboardingInstanceCompleteRequest request =
                payload == null || payload.isNull()
                        ? null
                        : objectMapper.convertValue(payload, OnboardingInstanceCompleteRequest.class);

        validate(context, request);

        String companyId = context.getTenantId();
        String mode = resolveManagerEvaluationMode(request.getManagerEvaluationMode());

        OnboardingInstanceEntity instance =
                onboardingInstanceMapper.selectByPrimaryKey(request.getInstanceId().trim());

        if (instance == null) {
            throw AppException.of(ErrorCodes.NOT_FOUND, "onboarding instance not found");
        }

        if (!companyId.equals(instance.getCompanyId())) {
            throw AppException.of(ErrorCodes.FORBIDDEN, "instance does not belong to tenant");
        }

        if ("DONE".equalsIgnoreCase(instance.getStatus())
                || "COMPLETED".equalsIgnoreCase(instance.getStatus())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "onboarding instance already completed");
        }

        if ("CANCELLED".equalsIgnoreCase(instance.getStatus())
                || "CANCELED".equalsIgnoreCase(instance.getStatus())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "cannot complete cancelled onboarding");
        }

        List<TaskInstanceEntity> tasks =
                taskInstanceMapper.selectByCompanyIdAndOnboardingId(companyId, instance.getOnboardingId());

        if (tasks == null || tasks.isEmpty()) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "cannot complete onboarding without generated tasks");
        }

        boolean hasIncompleteTask = tasks.stream()
                .anyMatch(task -> !OnboardingInstanceProgressService.isEffectivelyComplete(task));

        if (hasIncompleteTask) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "cannot complete onboarding while tasks are pending");
        }

        if (MODE_SEND_NOW.equals(mode)) {
            managerOnboardingEvaluationService.validateCanSendAfterOnboardingCompleted(
                    companyId,
                    instance,
                    request.getManagerEvaluationTemplateId(),
                    request.getManagerEvaluationDueDays()
            );
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

        String employeeUserId = resolveEmployeeUserId(instance.getEmployeeId());

        if (StringUtils.hasText(employeeUserId)) {
            IdentityUpdateUserContext identityContext = new IdentityUpdateUserContext();
            identityContext.setTenantId(context.getTenantId());
            identityContext.setOperatorId(context.getOperatorId());

            UpdateUserRequest updateUserRequest = UpdateUserRequest.builder()
                    .userId(employeeUserId)
                    .companyId(companyId)
                    .status("OFFICIAL")
                    .build();

            identityUserUpdateProcessor.process(identityContext, updateUserRequest);
        }

        OnboardingInstanceResponse response = new OnboardingInstanceResponse();
        response.setInstanceId(instance.getOnboardingId());
        response.setStatus(instance.getStatus());

        if (MODE_SEND_NOW.equals(mode)) {
            ManagerEvaluationSendResult evaluationResult =
                    managerOnboardingEvaluationService.sendAfterOnboardingCompleted(
                            companyId,
                            context.getOperatorId(),
                            instance,
                            request.getManagerEvaluationTemplateId(),
                            request.getManagerEvaluationDueDays()
                    );

            response.setManagerEvaluationSurveyInstanceId(evaluationResult.getSurveyInstanceId());
            response.setManagerEvaluationStatus(evaluationResult.getStatus());
            response.setManagerEvaluationMessage(evaluationResult.getMessage());
        } else {
            response.setManagerEvaluationStatus("PENDING");
            response.setManagerEvaluationMessage("Manager evaluation will be sent later");
        }

        return response;
    }

    private String resolveEmployeeUserId(String employeeIdRaw) {
        if (!StringUtils.hasText(employeeIdRaw)) {
            return null;
        }

        String key = employeeIdRaw.trim();

        EmployeeProfileEntity profile = employeeProfileMapper.selectByPrimaryKey(key);
        if (profile != null && StringUtils.hasText(profile.getUserId())) {
            return profile.getUserId().trim();
        }

        /*
         * Compatibility:
         * Data cũ/demo có thể lưu onboarding_instances.employee_id = users.user_id.
         */
        return key;
    }

    private static String resolveManagerEvaluationMode(String rawMode) {
        if (!StringUtils.hasText(rawMode)) {
            return MODE_SEND_NOW;
        }

        String mode = rawMode.trim().toUpperCase();

        if (!MODE_SEND_NOW.equals(mode) && !MODE_SEND_LATER.equals(mode)) {
            throw AppException.of(
                    ErrorCodes.BAD_REQUEST,
                    "invalid managerEvaluationMode. Allowed values: SEND_NOW, SEND_LATER"
            );
        }

        return mode;
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