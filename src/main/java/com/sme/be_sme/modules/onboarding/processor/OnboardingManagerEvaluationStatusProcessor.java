package com.sme.be_sme.modules.onboarding.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.employee.infrastructure.mapper.EmployeeProfileMapper;
import com.sme.be_sme.modules.employee.infrastructure.mapper.EmployeeProfileMapperExt;
import com.sme.be_sme.modules.employee.infrastructure.persistence.entity.EmployeeProfileEntity;
import com.sme.be_sme.modules.identity.infrastructure.mapper.UserMapperExt;
import com.sme.be_sme.modules.identity.infrastructure.persistence.entity.UserEntity;
import com.sme.be_sme.modules.onboarding.api.request.OnboardingManagerEvaluationStatusRequest;
import com.sme.be_sme.modules.onboarding.api.response.OnboardingManagerEvaluationStatusResponse;
import com.sme.be_sme.modules.onboarding.infrastructure.mapper.OnboardingInstanceMapper;
import com.sme.be_sme.modules.onboarding.infrastructure.persistence.entity.OnboardingInstanceEntity;
import com.sme.be_sme.modules.survey.infrastructure.mapper.SurveyInstanceMapperExt;
import com.sme.be_sme.modules.survey.infrastructure.persistence.entity.SurveyInstanceEntity;
import com.sme.be_sme.shared.constant.ErrorCodes;
import com.sme.be_sme.shared.exception.AppException;
import com.sme.be_sme.shared.gateway.core.BaseBizProcessor;
import com.sme.be_sme.shared.gateway.core.BizContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class OnboardingManagerEvaluationStatusProcessor extends BaseBizProcessor<BizContext> {

    private final ObjectMapper objectMapper;
    private final OnboardingInstanceMapper onboardingInstanceMapper;
    private final SurveyInstanceMapperExt surveyInstanceMapperExt;
    private final EmployeeProfileMapper employeeProfileMapper;
    private final EmployeeProfileMapperExt employeeProfileMapperExt;
    private final UserMapperExt userMapperExt;

    @Override
    protected Object doProcess(BizContext context, JsonNode payload) {
        OnboardingManagerEvaluationStatusRequest request =
                objectMapper.convertValue(payload, OnboardingManagerEvaluationStatusRequest.class);

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

        EmployeeProfileEntity employeeProfile = resolveEmployeeProfile(companyId, instance);
        UserEntity targetEmployeeUser = resolveTargetEmployeeUser(companyId, instance, employeeProfile);

        String targetEmployeeUserId = resolveTargetEmployeeUserId(instance, employeeProfile, targetEmployeeUser);
        String targetEmployeeName = resolveTargetEmployeeName(employeeProfile, targetEmployeeUser);
        String targetEmployeeEmail = resolveTargetEmployeeEmail(employeeProfile, targetEmployeeUser);

        String managerUserId = resolveManagerUserId(instance, employeeProfile);
        UserEntity managerUser = resolveUser(companyId, managerUserId);

        OnboardingManagerEvaluationStatusResponse response =
                new OnboardingManagerEvaluationStatusResponse();

        response.setInstanceId(instance.getOnboardingId());
        response.setOnboardingStatus(instance.getStatus());

        response.setManagerUserId(managerUserId);
        response.setManagerName(managerUser == null ? null : managerUser.getFullName());

        response.setTargetEmployeeUserId(targetEmployeeUserId);
        response.setTargetEmployeeName(targetEmployeeName);
        response.setTargetEmployeeEmail(targetEmployeeEmail);

        if (!"DONE".equalsIgnoreCase(instance.getStatus())
                && !"COMPLETED".equalsIgnoreCase(instance.getStatus())) {
            response.setManagerEvaluationStatus("SKIPPED");
            response.setMessage("Onboarding is not completed yet");
            return response;
        }

        SurveyInstanceEntity existed =
                surveyInstanceMapperExt.findExistingManagerEvaluationByOnboardingId(
                        companyId,
                        instance.getOnboardingId()
                );

        if (existed == null) {
            response.setManagerEvaluationStatus("PENDING");
            response.setMessage("Manager evaluation has not been sent yet");
            return response;
        }

        response.setManagerEvaluationSurveyInstanceId(existed.getSurveyInstanceId());

        String surveyStatus = existed.getStatus();

        if ("COMPLETED".equalsIgnoreCase(surveyStatus)
                || "SUBMITTED".equalsIgnoreCase(surveyStatus)
                || "DONE".equalsIgnoreCase(surveyStatus)) {
            response.setManagerEvaluationStatus("SUBMITTED");
            response.setMessage("Manager evaluation has been submitted");
        } else {
            response.setManagerEvaluationStatus("SENT");
            response.setMessage("Manager evaluation has been sent");
        }

        return response;
    }

    private EmployeeProfileEntity resolveEmployeeProfile(
            String companyId,
            OnboardingInstanceEntity instance
    ) {
        if (instance == null || !StringUtils.hasText(instance.getEmployeeId())) {
            return null;
        }

        String rawEmployeeId = instance.getEmployeeId().trim();

        /*
         * Case chuẩn:
         * onboarding_instances.employee_id = employee_profiles.employee_id
         */
        EmployeeProfileEntity byEmployeeId =
                employeeProfileMapper.selectByPrimaryKey(rawEmployeeId);

        if (byEmployeeId != null && companyId.equals(byEmployeeId.getCompanyId())) {
            return byEmployeeId;
        }

        /*
         * Case data cũ/demo:
         * onboarding_instances.employee_id = users.user_id
         */
        return employeeProfileMapperExt.selectByCompanyIdAndUserId(
                companyId,
                rawEmployeeId
        );
    }

    private UserEntity resolveTargetEmployeeUser(
            String companyId,
            OnboardingInstanceEntity instance,
            EmployeeProfileEntity employeeProfile
    ) {
        String userId = null;

        if (employeeProfile != null && StringUtils.hasText(employeeProfile.getUserId())) {
            userId = employeeProfile.getUserId().trim();
        } else if (instance != null && StringUtils.hasText(instance.getEmployeeId())) {
            /*
             * Fallback data cũ: employee_id chính là users.user_id
             */
            userId = instance.getEmployeeId().trim();
        }

        return resolveUser(companyId, userId);
    }

    private String resolveTargetEmployeeUserId(
            OnboardingInstanceEntity instance,
            EmployeeProfileEntity employeeProfile,
            UserEntity targetEmployeeUser
    ) {
        if (employeeProfile != null && StringUtils.hasText(employeeProfile.getUserId())) {
            return employeeProfile.getUserId().trim();
        }

        if (targetEmployeeUser != null && StringUtils.hasText(targetEmployeeUser.getUserId())) {
            return targetEmployeeUser.getUserId().trim();
        }

        if (instance != null && StringUtils.hasText(instance.getEmployeeId())) {
            return instance.getEmployeeId().trim();
        }

        return null;
    }

    private String resolveTargetEmployeeName(
            EmployeeProfileEntity employeeProfile,
            UserEntity targetEmployeeUser
    ) {
        if (employeeProfile != null && StringUtils.hasText(employeeProfile.getEmployeeName())) {
            return employeeProfile.getEmployeeName().trim();
        }

        if (targetEmployeeUser != null && StringUtils.hasText(targetEmployeeUser.getFullName())) {
            return targetEmployeeUser.getFullName().trim();
        }

        return null;
    }

    private String resolveTargetEmployeeEmail(
            EmployeeProfileEntity employeeProfile,
            UserEntity targetEmployeeUser
    ) {
        if (employeeProfile != null && StringUtils.hasText(employeeProfile.getEmployeeEmail())) {
            return employeeProfile.getEmployeeEmail().trim();
        }

        if (targetEmployeeUser != null && StringUtils.hasText(targetEmployeeUser.getEmail())) {
            return targetEmployeeUser.getEmail().trim();
        }

        return null;
    }

    private String resolveManagerUserId(
            OnboardingInstanceEntity instance,
            EmployeeProfileEntity employeeProfile
    ) {
        if (instance != null && StringUtils.hasText(instance.getManagerUserId())) {
            return instance.getManagerUserId().trim();
        }

        if (employeeProfile != null && StringUtils.hasText(employeeProfile.getManagerUserId())) {
            return employeeProfile.getManagerUserId().trim();
        }

        return null;
    }

    private UserEntity resolveUser(String companyId, String userId) {
        if (!StringUtils.hasText(companyId) || !StringUtils.hasText(userId)) {
            return null;
        }

        return userMapperExt.selectByCompanyIdAndUserId(
                companyId,
                userId.trim()
        );
    }
}