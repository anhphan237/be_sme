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
        final Map<String, EmployeeLinkInfo> employeeLinkByOnboardingEmployeeId = new HashMap<>();
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
                        employeeLinkByOnboardingEmployeeId,
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
            Map<String, EmployeeLinkInfo> employeeLinkByOnboardingEmployeeId,
            Map<String, String> managerNameByUserId
    ) {
        OnboardingInstanceDetailResponse response = new OnboardingInstanceDetailResponse();
        response.setInstanceId(entity.getOnboardingId());
        response.setEmployeeId(entity.getEmployeeId());
        EmployeeLinkInfo employeeLinkInfo = resolveEmployeeLinkInfo(
                companyId,
                entity.getEmployeeId(),
                employeeLinkByOnboardingEmployeeId,
                managerNameByUserId
        );
        response.setEmployeeUserId(employeeLinkInfo.employeeUserId);
        response.setManagerUserId(employeeLinkInfo.managerUserId);
        response.setManagerName(employeeLinkInfo.managerName);
        response.setTemplateId(entity.getOnboardingTemplateId());
        response.setStatus(entity.getStatus());
        response.setStartDate(entity.getStartDate());
        response.setCompletedAt(entity.getCompletedAt());
        return response;
    }

    private EmployeeLinkInfo resolveEmployeeLinkInfo(
            String companyId,
            String onboardingEmployeeId,
            Map<String, EmployeeLinkInfo> employeeLinkByOnboardingEmployeeId,
            Map<String, String> managerNameByUserId
    ) {
        String normalizedId = normalize(onboardingEmployeeId);
        if (!StringUtils.hasText(normalizedId)) {
            return EmployeeLinkInfo.empty();
        }
        if (employeeLinkByOnboardingEmployeeId.containsKey(normalizedId)) {
            return employeeLinkByOnboardingEmployeeId.get(normalizedId);
        }

        EmployeeProfileEntity profileByEmployeeId = employeeProfileMapper.selectByPrimaryKey(normalizedId);
        if (profileByEmployeeId != null && StringUtils.hasText(profileByEmployeeId.getUserId())) {
            String employeeUserId = normalize(profileByEmployeeId.getUserId());
            String managerUserId = normalize(profileByEmployeeId.getManagerUserId());
            String managerName = resolveManagerName(companyId, managerUserId, managerNameByUserId);
            EmployeeLinkInfo link = new EmployeeLinkInfo(employeeUserId, managerUserId, managerName);
            employeeLinkByOnboardingEmployeeId.put(normalizedId, link);
            return link;
        }

        EmployeeProfileEntity profileByUserId = employeeProfileMapperExt.selectByCompanyIdAndUserId(companyId, normalizedId);
        if (profileByUserId != null && StringUtils.hasText(profileByUserId.getUserId())) {
            String employeeUserId = normalize(profileByUserId.getUserId());
            String managerUserId = normalize(profileByUserId.getManagerUserId());
            String managerName = resolveManagerName(companyId, managerUserId, managerNameByUserId);
            EmployeeLinkInfo link = new EmployeeLinkInfo(employeeUserId, managerUserId, managerName);
            employeeLinkByOnboardingEmployeeId.put(normalizedId, link);
            return link;
        }

        // Compatibility: old onboarding_instances.employee_id may already store identity user_id.
        EmployeeLinkInfo link = new EmployeeLinkInfo(normalizedId, null, null);
        employeeLinkByOnboardingEmployeeId.put(normalizedId, link);
        return link;
    }

    private String resolveManagerName(String companyId, String managerUserId, Map<String, String> managerNameByUserId) {
        if (!StringUtils.hasText(managerUserId)) {
            return null;
        }
        if (managerNameByUserId.containsKey(managerUserId)) {
            return managerNameByUserId.get(managerUserId);
        }
        UserEntity manager = userMapperExt.selectByCompanyIdAndUserId(companyId, managerUserId);
        String managerName = manager == null ? null : normalize(manager.getFullName());
        managerNameByUserId.put(managerUserId, managerName);
        return managerName;
    }

    private static class EmployeeLinkInfo {
        private final String employeeUserId;
        private final String managerUserId;
        private final String managerName;

        private EmployeeLinkInfo(String employeeUserId, String managerUserId, String managerName) {
            this.employeeUserId = employeeUserId;
            this.managerUserId = managerUserId;
            this.managerName = managerName;
        }

        private static EmployeeLinkInfo empty() {
            return new EmployeeLinkInfo(null, null, null);
        }
    }
}
