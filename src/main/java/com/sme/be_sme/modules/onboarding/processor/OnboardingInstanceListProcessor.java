package com.sme.be_sme.modules.onboarding.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.employee.infrastructure.mapper.EmployeeProfileMapper;
import com.sme.be_sme.modules.employee.infrastructure.mapper.EmployeeProfileMapperExt;
import com.sme.be_sme.modules.employee.infrastructure.persistence.entity.EmployeeProfileEntity;
import com.sme.be_sme.modules.identity.infrastructure.mapper.UserMapperExt;
import com.sme.be_sme.modules.identity.infrastructure.persistence.entity.UserEntity;
import com.sme.be_sme.modules.onboarding.api.request.OnboardingInstanceListRequest;
import com.sme.be_sme.modules.onboarding.api.response.OnboardingInstanceDetailResponse;
import com.sme.be_sme.modules.onboarding.api.response.OnboardingInstanceListResponse;
import com.sme.be_sme.modules.onboarding.infrastructure.mapper.OnboardingInstanceMapper;
import com.sme.be_sme.modules.onboarding.infrastructure.persistence.entity.OnboardingInstanceEntity;
import com.sme.be_sme.shared.constant.ErrorCodes;
import com.sme.be_sme.shared.exception.AppException;
import com.sme.be_sme.shared.gateway.core.BaseBizProcessor;
import com.sme.be_sme.shared.gateway.core.BizContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class OnboardingInstanceListProcessor extends BaseBizProcessor<BizContext> {

    private static final String STATUS_ALL = "ALL";
    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_DONE = "DONE";
    private static final String STATUS_COMPLETED = "COMPLETED";
    private static final String STATUS_CANCELLED = "CANCELLED";
    private static final String STATUS_CANCELED = "CANCELED";

    private final ObjectMapper objectMapper;
    private final OnboardingInstanceMapper onboardingInstanceMapper;
    private final EmployeeProfileMapper employeeProfileMapper;
    private final EmployeeProfileMapperExt employeeProfileMapperExt;
    private final UserMapperExt userMapperExt;

    @Override
    protected Object doProcess(BizContext context, JsonNode payload) {
        validate(context);

        OnboardingInstanceListRequest request =
                payload == null || payload.isNull()
                        ? new OnboardingInstanceListRequest()
                        : objectMapper.convertValue(payload, OnboardingInstanceListRequest.class);

        String companyId = context.getTenantId();
        String requestedEmployeeId = request == null ? null : normalize(request.getEmployeeId());
        String requestedStatus = request == null ? null : normalizeStatus(request.getStatus());

        final boolean employeeScope = isEmployeeOnlyRole(context);
        final String operatorId = normalize(context.getOperatorId());

        if (employeeScope && !StringUtils.hasText(operatorId)) {
            throw AppException.of(ErrorCodes.FORBIDDEN, "employee context is required");
        }

        final String employeeId = employeeScope
                ? resolveEmployeeIdForOperator(context)
                : requestedEmployeeId;

        final Map<String, EmployeeLinkInfo> employeeLinkByOnboardingEmployeeId = new HashMap<>();
        final Map<String, String> managerNameByUserId = new HashMap<>();

        List<OnboardingInstanceDetailResponse> instances = onboardingInstanceMapper.selectAll().stream()
                .filter(row -> Objects.equals(companyId, row.getCompanyId()))
                .filter(row -> matchesEmployeeScope(row, employeeScope, employeeId, operatorId))
                .filter(row -> matchesStatus(row.getStatus(), requestedStatus))
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

    private boolean matchesEmployeeScope(
            OnboardingInstanceEntity row,
            boolean employeeScope,
            String employeeId,
            String operatorId
    ) {
        String rowEmployeeId = normalize(row.getEmployeeId());

        if (employeeScope) {
            if (!StringUtils.hasText(rowEmployeeId)) {
                return false;
            }

            /*
             * Compatibility:
             * - Data chuẩn: onboarding_instances.employee_id = employee_profiles.employee_id
             * - Data cũ/demo: onboarding_instances.employee_id = users.user_id
             */
            return Objects.equals(employeeId, rowEmployeeId)
                    || Objects.equals(operatorId, rowEmployeeId);
        }

        if (!StringUtils.hasText(employeeId)) {
            return true;
        }

        return Objects.equals(employeeId, rowEmployeeId);
    }

    private boolean matchesStatus(String rowStatusRaw, String requestedStatus) {
        String rowStatus = normalizeStatus(rowStatusRaw);


        if (!StringUtils.hasText(requestedStatus) || STATUS_ALL.equalsIgnoreCase(requestedStatus)) {
            return true;
        }

        if (STATUS_COMPLETED.equalsIgnoreCase(requestedStatus)) {
            return STATUS_DONE.equalsIgnoreCase(rowStatus)
                    || STATUS_COMPLETED.equalsIgnoreCase(rowStatus);
        }

        if (STATUS_CANCELLED.equalsIgnoreCase(requestedStatus)
                || STATUS_CANCELED.equalsIgnoreCase(requestedStatus)) {
            return STATUS_CANCELLED.equalsIgnoreCase(rowStatus)
                    || STATUS_CANCELED.equalsIgnoreCase(rowStatus);
        }

        return requestedStatus.equalsIgnoreCase(rowStatus);
    }

    private boolean isEmployeeOnlyRole(BizContext context) {
        if (context == null || context.getRoles() == null) {
            return false;
        }

        boolean hasEmployee = context.getRoles().stream()
                .anyMatch(role -> "EMPLOYEE".equalsIgnoreCase(role));

        boolean hasHighPrivilege = context.getRoles().stream()
                .anyMatch(role ->
                        "HR".equalsIgnoreCase(role)
                                || "HR_ADMIN".equalsIgnoreCase(role)
                                || "ADMIN".equalsIgnoreCase(role)
                                || "ADMIN_PLATFORM".equalsIgnoreCase(role)
                                || "PLATFORM_ADMIN".equalsIgnoreCase(role)
                                || "MANAGER".equalsIgnoreCase(role)
                );

        return hasEmployee && !hasHighPrivilege;
    }

    private String resolveEmployeeIdForOperator(BizContext context) {
        if (context == null || !StringUtils.hasText(context.getOperatorId())) {
            return null;
        }

        EmployeeProfileEntity me = employeeProfileMapperExt.selectByCompanyIdAndUserId(
                context.getTenantId(),
                context.getOperatorId()
        );

        if (me == null || !StringUtils.hasText(me.getEmployeeId())) {
            /*
             * Compatibility:
             * Nếu không có employee profile thì dùng luôn operatorId.
             * Vì data cũ có thể lưu onboarding_instances.employee_id = users.user_id.
             */
            return normalize(context.getOperatorId());
        }

        return normalize(me.getEmployeeId());
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String normalizeStatus(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }

        String status = value.trim().toUpperCase();

        if (STATUS_COMPLETED.equals(status)) {
            return STATUS_DONE;
        }

        if (STATUS_CANCELED.equals(status)) {
            return STATUS_CANCELLED;
        }

        return status;
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
        String managerUserId = StringUtils.hasText(entity.getManagerUserId())
                ? entity.getManagerUserId().trim()
                : employeeLinkInfo.managerUserId;
        response.setManagerUserId(managerUserId);
        response.setManagerName(resolveManagerName(companyId, managerUserId, managerNameByUserId));
        response.setTemplateId(entity.getOnboardingTemplateId());
        response.setStatus(entity.getStatus());
        response.setStartDate(entity.getStartDate());
        response.setCompletedAt(entity.getCompletedAt());
        response.setProgressPercent(entity.getProgressPercent() != null ? entity.getProgressPercent() : 0);
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

        EmployeeProfileEntity profileByUserId =
                employeeProfileMapperExt.selectByCompanyIdAndUserId(companyId, normalizedId);

        if (profileByUserId != null && StringUtils.hasText(profileByUserId.getUserId())) {
            String employeeUserId = normalize(profileByUserId.getUserId());
            String managerUserId = normalize(profileByUserId.getManagerUserId());
            String managerName = resolveManagerName(companyId, managerUserId, managerNameByUserId);

            EmployeeLinkInfo link = new EmployeeLinkInfo(employeeUserId, managerUserId, managerName);
            employeeLinkByOnboardingEmployeeId.put(normalizedId, link);
            return link;
        }

        EmployeeLinkInfo link = new EmployeeLinkInfo(normalizedId, null, null);
        employeeLinkByOnboardingEmployeeId.put(normalizedId, link);
        return link;
    }

    private String resolveManagerName(
            String companyId,
            String managerUserId,
            Map<String, String> managerNameByUserId
    ) {
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