package com.sme.be_sme.modules.onboarding.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.onboarding.api.request.OnboardingInstanceActivateRequest;
import com.sme.be_sme.modules.onboarding.api.request.OnboardingTaskGenerateRequest;
import com.sme.be_sme.modules.onboarding.api.response.OnboardingInstanceResponse;
import com.sme.be_sme.modules.onboarding.facade.OnboardingTaskFacade;
import com.sme.be_sme.modules.notification.service.NotificationCreateParams;
import com.sme.be_sme.modules.notification.service.NotificationService;
import com.sme.be_sme.modules.company.infrastructure.mapper.CompanyMapper;
import com.sme.be_sme.modules.company.infrastructure.persistence.entity.CompanyEntity;
import com.sme.be_sme.modules.employee.infrastructure.mapper.EmployeeProfileMapperExt;
import com.sme.be_sme.modules.employee.infrastructure.persistence.entity.EmployeeProfileEntity;
import com.sme.be_sme.modules.onboarding.infrastructure.mapper.OnboardingInstanceMapper;
import com.sme.be_sme.modules.onboarding.infrastructure.persistence.entity.OnboardingInstanceEntity;
import com.sme.be_sme.shared.constant.ErrorCodes;
import com.sme.be_sme.shared.exception.AppException;
import com.sme.be_sme.shared.gateway.core.BaseBizProcessor;
import com.sme.be_sme.shared.gateway.core.BizContext;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
@Slf4j
public class OnboardingInstanceActivateProcessor extends BaseBizProcessor<BizContext> {

    private static final String TEMPLATE_WELCOME = "WELCOME_NEW_EMPLOYEE";

    private final ObjectMapper objectMapper;
    private final OnboardingInstanceMapper onboardingInstanceMapper;
    private final OnboardingTaskFacade onboardingTaskFacade;
    private final NotificationService notificationService;
    private final EmployeeProfileMapperExt employeeProfileMapperExt;
    private final CompanyMapper companyMapper;

    /**
     * Keep status update and task generation in one transaction so a failed generate
     * does not leave an ACTIVE instance with zero tasks.
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Object execute(BizContext context) {
        return super.execute(context);
    }

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

        if (StringUtils.hasText(request.getManagerUserId())) {
            instance.setManagerUserId(request.getManagerUserId().trim());
        }
        if (StringUtils.hasText(request.getItStaffUserId())) {
            instance.setItStaffUserId(request.getItStaffUserId().trim());
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
        instance.setUpdatedBy(context.getOperatorId());
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
            genReq.setManagerId(resolveManagerUserIdForGenerate(instance));
            genReq.setItStaffUserId(
                    StringUtils.hasText(instance.getItStaffUserId()) ? instance.getItStaffUserId().trim() : null);
            onboardingTaskFacade.generateTasksFromTemplate(genReq);
            notifyOnboardingStarted(companyId, instance);
        }

        OnboardingInstanceResponse response = new OnboardingInstanceResponse();
        response.setInstanceId(instance.getOnboardingId());
        response.setStatus(instance.getStatus());
        return response;
    }

    private String resolveManagerUserIdForGenerate(OnboardingInstanceEntity instance) {
        if (StringUtils.hasText(instance.getManagerUserId())) {
            return instance.getManagerUserId().trim();
        }
        if (!StringUtils.hasText(instance.getEmployeeId())) {
            return null;
        }
        EmployeeProfileEntity profile = employeeProfileMapperExt.selectByCompanyIdAndUserId(
                instance.getCompanyId(), instance.getEmployeeId().trim());
        if (profile == null || !StringUtils.hasText(profile.getManagerUserId())) {
            return null;
        }
        return profile.getManagerUserId().trim();
    }

    private void notifyOnboardingStarted(String companyId, OnboardingInstanceEntity instance) {
        if (!StringUtils.hasText(instance.getEmployeeId())) return;
        try {
            EmployeeProfileEntity employee = employeeProfileMapperExt.selectByCompanyIdAndUserId(
                    companyId, instance.getEmployeeId().trim());
            if (employee == null || !StringUtils.hasText(employee.getUserId())) return;
            String companyName = "";
            CompanyEntity company = companyMapper.selectByPrimaryKey(companyId);
            if (company != null && StringUtils.hasText(company.getName())) companyName = company.getName();
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("employeeName", StringUtils.hasText(employee.getEmployeeName()) ? employee.getEmployeeName() : "there");
            placeholders.put("companyName", companyName);
            NotificationCreateParams params = NotificationCreateParams.builder()
                    .companyId(companyId)
                    .userId(employee.getUserId())
                    .type("ONBOARDING_STARTED")
                    .title("Onboarding started")
                    .content("Welcome to " + companyName + ". Your onboarding has started.")
                    .refType("ONBOARDING")
                    .refId(instance.getOnboardingId())
                    .sendEmail(true)
                    .emailTemplate(TEMPLATE_WELCOME)
                    .emailPlaceholders(placeholders)
                    .toEmail(employee.getEmployeeEmail())
                    .onboardingId(instance.getOnboardingId())
                    .build();
            notificationService.create(params);
        } catch (Exception e) {
            log.warn("Onboarding notification failed for {}: {}", instance.getOnboardingId(), e.getMessage());
        }
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
