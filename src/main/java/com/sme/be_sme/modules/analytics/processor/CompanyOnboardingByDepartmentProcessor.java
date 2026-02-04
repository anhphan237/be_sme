package com.sme.be_sme.modules.analytics.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.analytics.api.request.CompanyOnboardingByDepartmentRequest;
import com.sme.be_sme.modules.analytics.api.response.CompanyOnboardingByDepartmentResponse;
import com.sme.be_sme.modules.analytics.api.response.DepartmentOnboardingStatsResponse;
import com.sme.be_sme.modules.company.infrastructure.mapper.DepartmentMapper;
import com.sme.be_sme.modules.company.infrastructure.persistence.entity.DepartmentEntity;
import com.sme.be_sme.modules.onboarding.infrastructure.mapper.TaskInstanceMapper;
import com.sme.be_sme.modules.onboarding.infrastructure.persistence.entity.TaskInstanceEntity;
import com.sme.be_sme.shared.constant.ErrorCodes;
import com.sme.be_sme.shared.exception.AppException;
import com.sme.be_sme.shared.gateway.core.BaseBizProcessor;
import com.sme.be_sme.shared.gateway.core.BizContext;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Date;
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
public class CompanyOnboardingByDepartmentProcessor extends BaseBizProcessor<BizContext> {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    private final ObjectMapper objectMapper;
    private final DepartmentMapper departmentMapper;
    private final TaskInstanceMapper taskInstanceMapper;

    @Override
    protected Object doProcess(BizContext context, JsonNode payload) {
        CompanyOnboardingByDepartmentRequest request =
                objectMapper.convertValue(payload, CompanyOnboardingByDepartmentRequest.class);
        validate(context, request);

        String companyId = resolveCompanyId(context, request);
        LocalDate startDate = parseDate(request.getStartDate(), "startDate");
        LocalDate endDate = parseDate(request.getEndDate(), "endDate");
        if (endDate.isBefore(startDate)) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "endDate must be >= startDate");
        }

        Date rangeStart = atStartOfDay(startDate);
        Date rangeEnd = atEndOfDay(endDate);

        Map<String, DepartmentEntity> departmentById = departmentMapper.selectByCompany(companyId).stream()
                .filter(Objects::nonNull)
                .filter(dep -> StringUtils.hasText(dep.getDepartmentId()))
                .collect(Collectors.toMap(DepartmentEntity::getDepartmentId, dep -> dep, (left, right) -> left));

        Map<String, DepartmentOnboardingStatsResponse> statsByDepartment = departmentById.values().stream()
                .map(dep -> {
                    DepartmentOnboardingStatsResponse response = new DepartmentOnboardingStatsResponse();
                    response.setDepartmentId(dep.getDepartmentId());
                    response.setDepartmentName(dep.getName());
                    response.setTotalTasks(0);
                    response.setCompletedTasks(0);
                    return response;
                })
                .collect(Collectors.toMap(DepartmentOnboardingStatsResponse::getDepartmentId, item -> item));

        List<TaskInstanceEntity> tasks = taskInstanceMapper.selectAll();
        for (TaskInstanceEntity task : tasks) {
            if (task == null || !companyId.equals(task.getCompanyId())) {
                continue;
            }
            String departmentId = task.getAssignedDepartmentId();
            if (!StringUtils.hasText(departmentId)) {
                continue;
            }
            Date createdAt = task.getCreatedAt();
            if (!isWithinRange(createdAt, rangeStart, rangeEnd)) {
                continue;
            }
            DepartmentOnboardingStatsResponse stats = statsByDepartment.get(departmentId);
            if (stats == null) {
                stats = new DepartmentOnboardingStatsResponse();
                stats.setDepartmentId(departmentId);
                stats.setDepartmentName(resolveDepartmentName(departmentById, departmentId));
                stats.setTotalTasks(0);
                stats.setCompletedTasks(0);
                statsByDepartment.put(departmentId, stats);
            }
            stats.setTotalTasks(stats.getTotalTasks() + 1);
            if (isCompleted(task)) {
                stats.setCompletedTasks(stats.getCompletedTasks() + 1);
            }
        }

        CompanyOnboardingByDepartmentResponse response = new CompanyOnboardingByDepartmentResponse();
        response.setCompanyId(companyId);
        response.setDepartments(statsByDepartment.values().stream().toList());
        return response;
    }

    private static void validate(BizContext context, CompanyOnboardingByDepartmentRequest request) {
        if (context == null || !StringUtils.hasText(context.getTenantId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "tenantId is required");
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
        if (StringUtils.hasText(request.getCompanyId())
                && !Objects.equals(request.getCompanyId().trim(), context.getTenantId().trim())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "companyId does not match tenant");
        }
    }

    private static String resolveCompanyId(BizContext context, CompanyOnboardingByDepartmentRequest request) {
        if (StringUtils.hasText(request.getCompanyId())) {
            return request.getCompanyId().trim();
        }
        return context.getTenantId().trim();
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

    private static boolean isCompleted(TaskInstanceEntity task) {
        if (task == null) {
            return false;
        }
        if (task.getCompletedAt() != null) {
            return true;
        }
        return task.getStatus() != null && "COMPLETED".equalsIgnoreCase(task.getStatus());
    }

    private static String resolveDepartmentName(Map<String, DepartmentEntity> departmentById, String departmentId) {
        if (departmentById == null) {
            return null;
        }
        DepartmentEntity dep = departmentById.get(departmentId);
        return dep == null ? null : dep.getName();
    }
}
