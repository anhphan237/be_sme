package com.sme.be_sme.modules.onboarding.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.identity.infrastructure.mapper.UserMapperExt;
import com.sme.be_sme.modules.identity.infrastructure.persistence.entity.UserEntity;
import com.sme.be_sme.modules.onboarding.api.request.OnboardingInstanceListRequest;
import com.sme.be_sme.modules.onboarding.api.response.OnboardingInstanceDetailResponse;
import com.sme.be_sme.modules.onboarding.api.response.OnboardingInstanceListResponse;
import com.sme.be_sme.modules.employee.infrastructure.mapper.EmployeeProfileMapper;
import com.sme.be_sme.modules.employee.infrastructure.mapper.EmployeeProfileMapperExt;
import com.sme.be_sme.modules.employee.infrastructure.persistence.entity.EmployeeProfileEntity;
import com.sme.be_sme.modules.onboarding.infrastructure.mapper.OnboardingInstanceMapper;
import com.sme.be_sme.modules.onboarding.infrastructure.persistence.entity.OnboardingInstanceEntity;
import com.sme.be_sme.shared.constant.ErrorCodes;
import com.sme.be_sme.shared.exception.AppException;
import com.sme.be_sme.shared.gateway.core.BaseBizProcessor;
import com.sme.be_sme.shared.gateway.core.BizContext;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class OnboardingInstanceListProcessor extends BaseBizProcessor<BizContext> {

    private final ObjectMapper objectMapper;
    private final OnboardingInstanceMapper onboardingInstanceMapper;
    private final EmployeeProfileMapper employeeProfileMapper;
    private final EmployeeProfileMapperExt employeeProfileMapperExt;
    private final UserMapperExt userMapperExt;

    @Override
    protected Object doProcess(BizContext context, JsonNode payload) {
        OnboardingInstanceListRequest request = objectMapper.convertValue(payload, OnboardingInstanceListRequest.class);
        validate(context);

        String companyId = context.getTenantId();
        final boolean employeeScope = isEmployeeRole(context);
        String requestedEmployeeId = request == null ? null : request.getEmployeeId();
        final String operatorId = normalize(context == null ? null : context.getOperatorId());
        if (employeeScope && !StringUtils.hasText(operatorId)) {
            throw AppException.of(ErrorCodes.FORBIDDEN, "employee context is required");
        }
        final String employeeId = employeeScope
                ? resolveEmployeeIdForOperator(context) // EMPLOYEE can only query own onboarding instances
                : requestedEmployeeId;
        String status = request == null ? null : request.getStatus();
        final String statusNormalized = status == null ? null : status.trim().toLowerCase(Locale.ROOT);
        final Map<String, EmployeeProfileEntity> employeeProfileByEmployeeId = new HashMap<>();
        final Map<String, EmployeeProfileEntity> employeeProfileByUserId = new HashMap<>();
        final Map<String, String> managerNameByUserId = new HashMap<>();

        List<OnboardingInstanceDetailResponse> instances = onboardingInstanceMapper.selectAll().stream()
                .filter(row -> Objects.equals(companyId, row.getCompanyId()))
                .filter(row -> {
                    if (employeeScope) {
                        if (!StringUtils.hasText(row.getEmployeeId())) return false;
                        String rowEmployeeId = normalize(row.getEmployeeId());
                        // Compatibility: old data may store userId in onboarding_instances.employee_id
                        return Objects.equals(employeeId, rowEmployeeId)
                                || Objects.equals(operatorId, rowEmployeeId);
                    }
                    return !StringUtils.hasText(employeeId) || employeeId.trim().equals(normalize(row.getEmployeeId()));
                })
                .filter(row -> !StringUtils.hasText(statusNormalized)
                        || (row.getStatus() != null && row.getStatus().trim().toLowerCase(Locale.ROOT).equals(statusNormalized)))
                .map(row -> toDetailResponse(
                        row,
                        companyId,
                        employeeProfileByEmployeeId,
                        employeeProfileByUserId,
                        managerNameByUserId
                ))
                .collect(Collectors.toList());

        OnboardingInstanceListResponse response = new OnboardingInstanceListResponse();
        response.setInstances(instances);
        return response;
    }

    private static void validate(BizContext context) {
        if (context == null || !StringUtils.hasText(context.getTenantId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "tenantId is required");
        }
    }

    private boolean isEmployeeRole(BizContext context) {
        if (context == null || context.getRoles() == null) return false;
        return context.getRoles().stream().anyMatch(r -> "EMPLOYEE".equalsIgnoreCase(r));
    }

    private String resolveEmployeeIdForOperator(BizContext context) {
        if (context == null || !StringUtils.hasText(context.getOperatorId())) return null;
        EmployeeProfileEntity me = employeeProfileMapperExt.selectByCompanyIdAndUserId(
                context.getTenantId(),
                context.getOperatorId()
        );
        if (me == null || !StringUtils.hasText(me.getEmployeeId())) return null;
        return normalize(me.getEmployeeId());
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private OnboardingInstanceDetailResponse toDetailResponse(
            OnboardingInstanceEntity entity,
            String companyId,
            Map<String, EmployeeProfileEntity> employeeProfileByEmployeeId,
            Map<String, EmployeeProfileEntity> employeeProfileByUserId,
            Map<String, String> managerNameByUserId
    ) {
        OnboardingInstanceDetailResponse response = new OnboardingInstanceDetailResponse();
        response.setInstanceId(entity.getOnboardingId());
        response.setEmployeeId(entity.getEmployeeId());
        EmployeeProfileEntity employeeProfile = resolveEmployeeProfile(
                companyId,
                entity.getEmployeeId(),
                employeeProfileByEmployeeId,
                employeeProfileByUserId
        );
        if (employeeProfile != null) {
            response.setEmployeeName(normalize(employeeProfile.getEmployeeName()));
            response.setEmployeeEmail(normalize(employeeProfile.getEmployeeEmail()));
            String managerUserId = normalize(employeeProfile.getManagerUserId());
            response.setManagerUserId(managerUserId);
            response.setManagerName(resolveManagerName(companyId, managerUserId, managerNameByUserId));
        }
        response.setTemplateId(entity.getOnboardingTemplateId());
        response.setStatus(entity.getStatus());
        response.setStartDate(entity.getStartDate());
        response.setCompletedAt(entity.getCompletedAt());
        return response;
    }

    private EmployeeProfileEntity resolveEmployeeProfile(
            String companyId,
            String onboardingEmployeeId,
            Map<String, EmployeeProfileEntity> employeeProfileByEmployeeId,
            Map<String, EmployeeProfileEntity> employeeProfileByUserId
    ) {
        String normalizedId = normalize(onboardingEmployeeId);
        if (!StringUtils.hasText(normalizedId)) return null;

        EmployeeProfileEntity profile = employeeProfileByEmployeeId.computeIfAbsent(
                normalizedId,
                employeeProfileMapper::selectByPrimaryKey
        );
        if (profile == null) {
            profile = employeeProfileByUserId.computeIfAbsent(
                    normalizedId,
                    userId -> employeeProfileMapperExt.selectByCompanyIdAndUserId(companyId, userId)
            );
        }
        if (profile != null) {
            String profileEmployeeId = normalize(profile.getEmployeeId());
            String profileUserId = normalize(profile.getUserId());
            if (StringUtils.hasText(profileEmployeeId)) {
                employeeProfileByEmployeeId.put(profileEmployeeId, profile);
            }
            if (StringUtils.hasText(profileUserId)) {
                employeeProfileByUserId.put(profileUserId, profile);
            }
        }
        return profile;
    }

    private String resolveManagerName(
            String companyId,
            String managerUserId,
            Map<String, String> managerNameByUserId
    ) {
        if (!StringUtils.hasText(managerUserId)) return null;
        return managerNameByUserId.computeIfAbsent(managerUserId, userId -> {
            UserEntity manager = userMapperExt.selectByCompanyIdAndUserId(companyId, userId);
            return manager == null ? null : normalize(manager.getFullName());
        });
    }
}
