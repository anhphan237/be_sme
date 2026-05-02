package com.sme.be_sme.modules.analytics.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.analytics.api.request.CompanyOnboardingTrendRequest;
import com.sme.be_sme.modules.analytics.api.request.OnboardingTrendGranularity;
import com.sme.be_sme.modules.analytics.api.response.CompanyOnboardingTrendPoint;
import com.sme.be_sme.modules.analytics.api.response.CompanyOnboardingTrendResponse;
import com.sme.be_sme.modules.onboarding.infrastructure.mapper.OnboardingInstanceMapper;
import com.sme.be_sme.modules.onboarding.infrastructure.persistence.entity.OnboardingInstanceEntity;
import com.sme.be_sme.modules.onboarding.support.OnboardingInstanceStatus;
import com.sme.be_sme.shared.constant.ErrorCodes;
import com.sme.be_sme.shared.exception.AppException;
import com.sme.be_sme.shared.gateway.core.BaseBizProcessor;
import com.sme.be_sme.shared.gateway.core.BizContext;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class CompanyOnboardingTrendProcessor extends BaseBizProcessor<BizContext> {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    private final ObjectMapper objectMapper;
    private final OnboardingInstanceMapper onboardingInstanceMapper;

    @Override
    protected Object doProcess(BizContext context, JsonNode payload) {
        CompanyOnboardingTrendRequest request =
                objectMapper.convertValue(payload, CompanyOnboardingTrendRequest.class);
        validate(context, request);

        OnboardingTrendGranularity gran = OnboardingTrendGranularity.fromPayload(request.getGranularity());
        String companyId = resolveCompanyId(context, request);
        LocalDate startDay = parseDate(request.getStartDate(), "startDate");
        LocalDate endDay = parseDate(request.getEndDate(), "endDate");
        if (endDay.isBefore(startDay)) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "endDate must be >= startDate");
        }

        ZoneId zone = ZoneId.systemDefault();
        Map<String, Counts> buckets = buildEmptyBuckets(gran, startDay, endDay);

        List<OnboardingInstanceEntity> instances = onboardingInstanceMapper.selectAll();
        for (OnboardingInstanceEntity instance : instances) {
            if (instance == null || !companyId.equals(instance.getCompanyId())) {
                continue;
            }

            Date createdRef = instance.getCreatedAt() != null ? instance.getCreatedAt() : instance.getStartDate();
            if (createdRef != null) {
                LocalDate ld = localDateOf(createdRef, zone);
                addCreated(buckets, gran, ld);
            }

            if (isCancelled(instance)) {
                Date cancelledRef =
                        instance.getUpdatedAt() != null ? instance.getUpdatedAt() : instance.getCreatedAt();
                if (cancelledRef == null) {
                    cancelledRef = instance.getStartDate();
                }
                if (cancelledRef != null) {
                    LocalDate ld = localDateOf(cancelledRef, zone);
                    addCancelled(buckets, gran, ld);
                }
            } else {
                boolean completionEvent =
                        instance.getCompletedAt() != null || isCompleted(instance);
                Date completedRef = instance.getCompletedAt();
                if (completedRef == null && isCompleted(instance)) {
                    completedRef = instance.getUpdatedAt();
                }
                if (completedRef != null && completionEvent) {
                    LocalDate ld = localDateOf(completedRef, zone);
                    addCompleted(buckets, gran, ld);
                }
            }
        }

        List<CompanyOnboardingTrendPoint> points = new ArrayList<>(buckets.size());
        for (Map.Entry<String, Counts> e : buckets.entrySet()) {
            CompanyOnboardingTrendPoint p = new CompanyOnboardingTrendPoint();
            p.setPeriod(e.getKey());
            Counts c = e.getValue();
            p.setCreatedCount(c.created);
            p.setCompletedCount(c.completed);
            p.setCancelledCount(c.cancelled);
            points.add(p);
        }

        CompanyOnboardingTrendResponse response = new CompanyOnboardingTrendResponse();
        response.setCompanyId(companyId);
        response.setGranularity(gran.name());
        response.setPoints(points);
        return response;
    }

    private static Map<String, Counts> buildEmptyBuckets(
            OnboardingTrendGranularity gran, LocalDate startDay, LocalDate endDay) {
        Map<String, Counts> buckets = new LinkedHashMap<>();
        switch (gran) {
            case DAY -> {
                for (LocalDate d = startDay; !d.isAfter(endDay); d = d.plusDays(1)) {
                    buckets.put(d.toString(), new Counts());
                }
            }
            case MONTH -> {
                YearMonth startYm = YearMonth.from(startDay);
                YearMonth endYm = YearMonth.from(endDay);
                for (YearMonth ym = startYm; !ym.isAfter(endYm); ym = ym.plusMonths(1)) {
                    buckets.put(ym.toString(), new Counts());
                }
            }
            case YEAR -> {
                int y0 = startDay.getYear();
                int y1 = endDay.getYear();
                for (int y = y0; y <= y1; y++) {
                    buckets.put(String.valueOf(y), new Counts());
                }
            }
        }
        return buckets;
    }

    private static String bucketKey(LocalDate ld, OnboardingTrendGranularity gran) {
        return switch (gran) {
            case DAY -> ld.toString();
            case MONTH -> YearMonth.from(ld).toString();
            case YEAR -> String.valueOf(ld.getYear());
        };
    }

    private static void addCreated(
            Map<String, Counts> buckets, OnboardingTrendGranularity gran, LocalDate eventDay) {
        Counts c = buckets.get(bucketKey(eventDay, gran));
        if (c != null) {
            c.created++;
        }
    }

    private static void addCompleted(
            Map<String, Counts> buckets, OnboardingTrendGranularity gran, LocalDate eventDay) {
        Counts c = buckets.get(bucketKey(eventDay, gran));
        if (c != null) {
            c.completed++;
        }
    }

    private static void addCancelled(
            Map<String, Counts> buckets, OnboardingTrendGranularity gran, LocalDate eventDay) {
        Counts c = buckets.get(bucketKey(eventDay, gran));
        if (c != null) {
            c.cancelled++;
        }
    }

    private static LocalDate localDateOf(Date d, ZoneId zone) {
        return d.toInstant().atZone(zone).toLocalDate();
    }

    private static boolean isCompleted(OnboardingInstanceEntity instance) {
        if (instance == null) {
            return false;
        }
        if (instance.getCompletedAt() != null) {
            return true;
        }
        if (instance.getStatus() == null) {
            return false;
        }
        String status = instance.getStatus().trim();
        return OnboardingInstanceStatus.DONE.equalsIgnoreCase(status)
                || OnboardingInstanceStatus.COMPLETED_LEGACY.equalsIgnoreCase(status);
    }

    private static boolean isCancelled(OnboardingInstanceEntity instance) {
        if (instance == null || instance.getStatus() == null) {
            return false;
        }
        String s = instance.getStatus().trim().toLowerCase(Locale.ROOT);
        return "cancelled".equals(s) || "canceled".equals(s);
    }

    private static void validate(BizContext context, CompanyOnboardingTrendRequest request) {
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
    }

    private static String resolveCompanyId(BizContext context, CompanyOnboardingTrendRequest request) {
        boolean platformAdmin = isPlatformAdmin(context);

        String tenantId = trimToNull(context.getTenantId());
        String requestCompanyId = trimToNull(request.getCompanyId());

        if (platformAdmin) {
            if (!StringUtils.hasText(requestCompanyId)) {
                throw AppException.of(ErrorCodes.BAD_REQUEST, "companyId is required for platform admin");
            }
            return requestCompanyId;
        }

        if (!StringUtils.hasText(tenantId)) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "tenantId is required");
        }

        if (StringUtils.hasText(requestCompanyId) && !Objects.equals(requestCompanyId, tenantId)) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "companyId does not match tenant");
        }

        return tenantId;
    }

    private static boolean isPlatformAdmin(BizContext context) {
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
                        "PLATFORM_ADMIN".equals(role)
                                || "ADMIN_PLATFORM".equals(role)
                                || "ADMIN".equals(role));
    }

    private static String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private static LocalDate parseDate(String value, String fieldName) {
        try {
            return LocalDate.parse(value.trim(), DATE_FORMATTER);
        } catch (DateTimeParseException ex) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, fieldName + " must be ISO-8601 yyyy-MM-dd");
        }
    }

    private static final class Counts {
        private int created;
        private int completed;
        private int cancelled;
    }
}
