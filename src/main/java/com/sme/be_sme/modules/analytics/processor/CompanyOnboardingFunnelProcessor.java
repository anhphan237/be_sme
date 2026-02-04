package com.sme.be_sme.modules.analytics.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.analytics.api.request.CompanyOnboardingFunnelRequest;
import com.sme.be_sme.modules.analytics.api.response.CompanyOnboardingFunnelResponse;
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
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class CompanyOnboardingFunnelProcessor extends BaseBizProcessor<BizContext> {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    private final ObjectMapper objectMapper;
    private final OnboardingInstanceMapper onboardingInstanceMapper;

    @Override
    protected Object doProcess(BizContext context, JsonNode payload) {
        CompanyOnboardingFunnelRequest request = objectMapper.convertValue(payload, CompanyOnboardingFunnelRequest.class);
        validate(context, request);

        String companyId = resolveCompanyId(context, request);
        LocalDate startDate = parseDate(request.getStartDate(), "startDate");
        LocalDate endDate = parseDate(request.getEndDate(), "endDate");
        if (endDate.isBefore(startDate)) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "endDate must be >= startDate");
        }

        Date rangeStart = atStartOfDay(startDate);
        Date rangeEnd = atEndOfDay(endDate);

        List<OnboardingInstanceEntity> instances = onboardingInstanceMapper.selectAll();
        int total = 0;
        int active = 0;
        int completed = 0;
        int cancelled = 0;
        int other = 0;

        for (OnboardingInstanceEntity instance : instances) {
            if (instance == null || !companyId.equals(instance.getCompanyId())) {
                continue;
            }
            Date startTimestamp = instance.getStartDate() != null ? instance.getStartDate() : instance.getCreatedAt();
            if (!isWithinRange(startTimestamp, rangeStart, rangeEnd)) {
                continue;
            }
            total++;
            String status = trimLower(instance.getStatus());
            if ("active".equals(status)) {
                active++;
            } else if ("completed".equals(status) || instance.getCompletedAt() != null) {
                completed++;
            } else if ("cancelled".equals(status) || "canceled".equals(status)) {
                cancelled++;
            } else {
                other++;
            }
        }

        CompanyOnboardingFunnelResponse response = new CompanyOnboardingFunnelResponse();
        response.setCompanyId(companyId);
        response.setTotalInstances(total);
        response.setActiveCount(active);
        response.setCompletedCount(completed);
        response.setCancelledCount(cancelled);
        response.setOtherCount(other);
        return response;
    }

    private static void validate(BizContext context, CompanyOnboardingFunnelRequest request) {
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

    private static String resolveCompanyId(BizContext context, CompanyOnboardingFunnelRequest request) {
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

    private static String trimLower(String value) {
        return value == null ? null : value.trim().toLowerCase(Locale.ROOT);
    }
}
