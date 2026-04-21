package com.sme.be_sme.modules.analytics.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.analytics.api.request.ManagerTeamSummaryRequest;
import com.sme.be_sme.modules.analytics.api.response.ManagerTeamSummaryResponse;
import com.sme.be_sme.modules.onboarding.infrastructure.mapper.OnboardingInstanceMapper;
import com.sme.be_sme.modules.onboarding.infrastructure.persistence.entity.OnboardingInstanceEntity;
import com.sme.be_sme.shared.constant.ErrorCodes;
import com.sme.be_sme.shared.exception.AppException;
import com.sme.be_sme.shared.gateway.core.BaseBizProcessor;
import com.sme.be_sme.shared.gateway.core.BizContext;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class ManagerTeamSummaryProcessor extends BaseBizProcessor<BizContext> {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    private final ObjectMapper objectMapper;
    private final OnboardingInstanceMapper onboardingInstanceMapper;

    @Override
    protected Object doProcess(BizContext context, JsonNode payload) {
        ManagerTeamSummaryRequest request = objectMapper.convertValue(payload, ManagerTeamSummaryRequest.class);
        validate(context, request);

        String companyId = resolveCompanyId(context, request);
        String managerUserId = resolveManagerUserId(context, request);

        LocalDate startDate = parseDate(request.getStartDate(), "startDate");
        LocalDate endDate = parseDate(request.getEndDate(), "endDate");

        if (endDate.isBefore(startDate)) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "endDate must be >= startDate");
        }

        Date rangeStart = atStartOfDay(startDate);
        Date rangeEnd = atEndOfDay(endDate);

        List<OnboardingInstanceEntity> instances = onboardingInstanceMapper.selectAll();

        Set<String> employeeIds = new HashSet<>();
        int totalOnboarding = 0;
        int activeOnboarding = 0;
        int completedOnboarding = 0;
        int cancelledOnboarding = 0;
        int progressSum = 0;
        int progressCount = 0;

        List<ManagerTeamSummaryResponse.AttentionEmployee> attentionEmployees = new ArrayList<>();

        for (OnboardingInstanceEntity instance : instances) {
            if (instance == null) {
                continue;
            }

            if (!Objects.equals(companyId, instance.getCompanyId())) {
                continue;
            }

            if (!Objects.equals(managerUserId, instance.getManagerUserId())) {
                continue;
            }

            Date startTimestamp = instance.getStartDate() != null ? instance.getStartDate() : instance.getCreatedAt();
            if (!isWithinRange(startTimestamp, rangeStart, rangeEnd)) {
                continue;
            }

            totalOnboarding++;

            if (StringUtils.hasText(instance.getEmployeeId())) {
                employeeIds.add(instance.getEmployeeId());
            }

            String status = trimUpper(instance.getStatus());

            if (isCompleted(instance)) {
                completedOnboarding++;
            } else if (isCancelled(status)) {
                cancelledOnboarding++;
            } else {
                activeOnboarding++;
            }

            Integer progress = instance.getProgressPercent();
            if (progress != null) {
                progressSum += progress;
                progressCount++;
            }

            ManagerTeamSummaryResponse.AttentionEmployee attention =
                    buildAttentionEmployee(instance, status, progress);

            if (attention != null) {
                attentionEmployees.add(attention);
            }
        }

        ManagerTeamSummaryResponse response = new ManagerTeamSummaryResponse();
        response.setCompanyId(companyId);
        response.setManagerUserId(managerUserId);
        response.setTeamMemberCount(employeeIds.size());
        response.setTotalOnboarding(totalOnboarding);
        response.setActiveOnboarding(activeOnboarding);
        response.setCompletedOnboarding(completedOnboarding);
        response.setCancelledOnboarding(cancelledOnboarding);
        response.setAverageProgress(progressCount == 0 ? 0.0 : progressSum * 1.0 / progressCount);
        response.setAttentionCount(attentionEmployees.size());
        response.setAttentionEmployees(attentionEmployees);

        return response;
    }

    private static void validate(BizContext context, ManagerTeamSummaryRequest request) {
        if (context == null) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "context is required");
        }
        if (request == null) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "payload is required");
        }
        if (!StringUtils.hasText(request.getStartDate())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "startDate is required");
        }
        if (!StringUtils.hasText(request.getEndDate())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "endDate is required");
        }

        resolveCompanyId(context, request);
        resolveManagerUserId(context, request);
    }

    private static String resolveCompanyId(BizContext context, ManagerTeamSummaryRequest request) {
        String tenantId = trimToNull(context.getTenantId());
        String requestCompanyId = trimToNull(request.getCompanyId());

        if (!StringUtils.hasText(tenantId)) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "tenantId is required");
        }

        if (StringUtils.hasText(requestCompanyId) && !Objects.equals(requestCompanyId, tenantId)) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "companyId does not match tenant");
        }

        return tenantId;
    }

    private static String resolveManagerUserId(BizContext context, ManagerTeamSummaryRequest request) {
        String operatorId = trimToNull(context.getOperatorId());
        String requestManagerUserId = trimToNull(request.getManagerUserId());

        boolean highPrivilege = isHighPrivilege(context);

        if (highPrivilege && StringUtils.hasText(requestManagerUserId)) {
            return requestManagerUserId;
        }

        if (!StringUtils.hasText(operatorId)) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "operatorId is required");
        }

        if (StringUtils.hasText(requestManagerUserId) && !Objects.equals(requestManagerUserId, operatorId)) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "managerUserId does not match operator");
        }

        return operatorId;
    }

    private static boolean isHighPrivilege(BizContext context) {
        if (context.getRoles() == null || context.getRoles().isEmpty()) {
            return false;
        }

        return context.getRoles().stream()
                .filter(Objects::nonNull)
                .map(role -> role.trim().toUpperCase(Locale.ROOT))
                .map(role -> role.replace("ROLE_", ""))
                .map(role -> role.replace(" ", "_"))
                .map(role -> role.replace("-", "_"))
                .anyMatch(role ->
                        "HR".equals(role)
                                || "HR_ADMIN".equals(role)
                                || "ADMIN".equals(role)
                                || "COMPANY_ADMIN".equals(role)
                                || "PLATFORM_ADMIN".equals(role)
                                || "ADMIN_PLATFORM".equals(role)
                );
    }

    private static ManagerTeamSummaryResponse.AttentionEmployee buildAttentionEmployee(
            OnboardingInstanceEntity instance,
            String status,
            Integer progress
    ) {
        if (instance == null) {
            return null;
        }

        if (isCompleted(instance) || isCancelled(status)) {
            return null;
        }

        String reason = null;

        if (progress != null && progress < 50) {
            reason = "LOW_PROGRESS";
        } else if ("OVERDUE".equals(status)) {
            reason = "OVERDUE";
        } else if ("RISK".equals(status) || "AT_RISK".equals(status)) {
            reason = "AT_RISK";
        }

        if (reason == null) {
            return null;
        }

        ManagerTeamSummaryResponse.AttentionEmployee item =
                new ManagerTeamSummaryResponse.AttentionEmployee();

        item.setOnboardingId(instance.getOnboardingId());
        item.setEmployeeId(instance.getEmployeeId());
        item.setStatus(instance.getStatus());
        item.setProgressPercent(progress == null ? 0 : progress);
        item.setReason(reason);

        return item;
    }

    private static boolean isCompleted(OnboardingInstanceEntity instance) {
        if (instance == null) {
            return false;
        }

        if (instance.getCompletedAt() != null) {
            return true;
        }

        String status = trimUpper(instance.getStatus());
        return "COMPLETED".equals(status)
                || "DONE".equals(status)
                || "CLOSED".equals(status);
    }

    private static boolean isCancelled(String status) {
        return "CANCELLED".equals(status)
                || "CANCELED".equals(status)
                || "CANCEL".equals(status);
    }

    private static LocalDate parseDate(String value, String fieldName) {
        try {
            return LocalDate.parse(value.trim(), DATE_FORMATTER);
        } catch (DateTimeParseException ex) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, fieldName + " must be ISO-8601 yyyy-MM-dd");
        }
    }

    private static Date atStartOfDay(LocalDate date) {
        return Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    private static Date atEndOfDay(LocalDate date) {
        return Date.from(date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    private static boolean isWithinRange(Date value, Date start, Date endExclusive) {
        if (value == null) {
            return false;
        }
        return !value.before(start) && value.before(endExclusive);
    }

    private static String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private static String trimUpper(String value) {
        return value == null ? null : value.trim().toUpperCase(Locale.ROOT);
    }
}