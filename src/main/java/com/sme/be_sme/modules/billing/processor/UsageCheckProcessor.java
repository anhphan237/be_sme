package com.sme.be_sme.modules.billing.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.billing.api.request.UsageCheckRequest;
import com.sme.be_sme.modules.billing.api.response.UsageCheckResponse;
import com.sme.be_sme.modules.billing.infrastructure.mapper.OnboardingUsageMapper;
import com.sme.be_sme.modules.billing.infrastructure.mapper.PlanMapper;
import com.sme.be_sme.modules.billing.infrastructure.mapper.SubscriptionMapper;
import com.sme.be_sme.modules.billing.infrastructure.persistence.entity.PlanEntity;
import com.sme.be_sme.modules.billing.infrastructure.persistence.entity.SubscriptionEntity;
import com.sme.be_sme.modules.billing.service.CompanyPlanQuotaService;
import com.sme.be_sme.modules.notification.service.NotificationCreateParams;
import com.sme.be_sme.modules.notification.service.NotificationService;
import com.sme.be_sme.shared.constant.ErrorCodes;
import com.sme.be_sme.shared.exception.AppException;
import com.sme.be_sme.shared.gateway.core.BaseBizProcessor;
import com.sme.be_sme.shared.gateway.core.BizContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class UsageCheckProcessor extends BaseBizProcessor<BizContext> {

    private static final String ALERT_NONE = "NONE";
    private static final String ALERT_APPROACHING = "APPROACHING";
    private static final String ALERT_EXCEEDED = "EXCEEDED";
    private static final double APPROACHING_THRESHOLD = 80.0;
    private static final double EXCEEDED_THRESHOLD = 100.0;
    private static final String NOTIFICATION_TYPE_USAGE_ALERT = "USAGE_ALERT";
    private static final String TEMPLATE_USAGE_ALERT = "USAGE_ALERT";

    private final ObjectMapper objectMapper;
    private final OnboardingUsageMapper onboardingUsageMapper;
    private final PlanMapper planMapper;
    private final SubscriptionMapper subscriptionMapper;
    private final NotificationService notificationService;
    private final CompanyPlanQuotaService companyPlanQuotaService;

    private static final DateTimeFormatter YEAR_MONTH = DateTimeFormatter.ofPattern("yyyy-MM");

    @Override
    protected Object doProcess(BizContext context, JsonNode payload) {
        UsageCheckRequest request = payload != null ? objectMapper.convertValue(payload, UsageCheckRequest.class) : null;
        validate(context);

        String companyId = context.getTenantId();
        String operatorId = context.getOperatorId();
        YearMonth ym = StringUtils.hasText(request != null ? request.getMonth() : null)
                ? YearMonth.parse(request.getMonth().trim(), YEAR_MONTH)
                : YearMonth.now();
        String month = ym.format(YEAR_MONTH);

        Date monthStart = Date.from(ym.atDay(1).atStartOfDay(ZoneId.systemDefault()).toInstant());
        Calendar cal = Calendar.getInstance(Locale.US);
        cal.setTime(monthStart);
        cal.add(Calendar.MONTH, 1);
        Date monthEnd = cal.getTime();

        int count = onboardingUsageMapper.countOnboardingInstancesByCompanyAndDateRange(companyId, monthStart, monthEnd);

        Integer limit = null;
        PlanEntity plan = resolvePlanForCompany(companyId);
        if (plan != null && plan.getEmployeeLimitPerMonth() != null && plan.getEmployeeLimitPerMonth() > 0) {
            limit = plan.getEmployeeLimitPerMonth();
        }

        String alertLevel = ALERT_NONE;
        Integer limitPercent = null;
        if (limit != null) {
            limitPercent = limit == 0 ? 0 : (int) Math.round(100.0 * count / limit);
            if (limitPercent >= EXCEEDED_THRESHOLD) {
                alertLevel = ALERT_EXCEEDED;
            } else if (limitPercent >= APPROACHING_THRESHOLD) {
                alertLevel = ALERT_APPROACHING;
            }
        }

        if (ALERT_APPROACHING.equals(alertLevel) || ALERT_EXCEEDED.equals(alertLevel)) {
            createUsageAlertNotification(companyId, operatorId, month, count, limit, alertLevel);
        }

        long currentStorageBytes = companyPlanQuotaService.getCurrentStorageBytes(companyId);
        Long storageLimit = plan != null && plan.getStorageLimitBytes() != null && plan.getStorageLimitBytes() > 0
                ? plan.getStorageLimitBytes()
                : null;
        String storageAlertLevel = ALERT_NONE;
        Integer storageLimitPercent = null;
        if (storageLimit != null) {
            storageLimitPercent = storageLimit == 0L ? 0 : (int) Math.round(100.0 * currentStorageBytes / storageLimit);
            if (storageLimitPercent >= EXCEEDED_THRESHOLD) {
                storageAlertLevel = ALERT_EXCEEDED;
            } else if (storageLimitPercent >= APPROACHING_THRESHOLD) {
                storageAlertLevel = ALERT_APPROACHING;
            }
        }

        UsageCheckResponse response = new UsageCheckResponse();
        response.setCurrentUsage(count);
        response.setMonth(month);
        response.setEmployeeLimitPerMonth(limit);
        response.setAlertLevel(alertLevel);
        response.setLimitPercent(limitPercent);
        response.setCurrentStorageBytes(currentStorageBytes);
        response.setStorageLimitBytes(storageLimit);
        response.setStorageAlertLevel(storageAlertLevel);
        response.setStorageLimitPercent(storageLimitPercent);
        return response;
    }

    private PlanEntity resolvePlanForCompany(String companyId) {
        List<SubscriptionEntity> list = subscriptionMapper.selectAll();
        if (list == null) return null;
        SubscriptionEntity current = list.stream()
                .filter(Objects::nonNull)
                .filter(s -> companyId.equals(s.getCompanyId()))
                .filter(s -> "ACTIVE".equalsIgnoreCase(trimLower(s.getStatus())))
                .max(Comparator.comparing(SubscriptionEntity::getUpdatedAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .orElse(null);
        if (current == null || !StringUtils.hasText(current.getPlanId())) return null;
        return planMapper.selectByPrimaryKey(current.getPlanId());
    }

    private void createUsageAlertNotification(String companyId, String userId, String month, int usage, Integer limit, String alertLevel) {
        if (!StringUtils.hasText(userId)) return;
        String title = ALERT_EXCEEDED.equals(alertLevel) ? "Usage limit exceeded" : "Usage approaching limit";
        String content = String.format("Onboarding usage for %s: %d of %s. %s.", month, usage, limit != null ? limit.toString() : "—", ALERT_EXCEEDED.equals(alertLevel) ? "Limit exceeded" : "Approaching limit");
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("alertTitle", title);
        placeholders.put("alertContent", content);
        NotificationCreateParams params = NotificationCreateParams.builder()
                .companyId(companyId)
                .userId(userId)
                .type(NOTIFICATION_TYPE_USAGE_ALERT)
                .title(title)
                .content(content)
                .refType("USAGE")
                .refId(month)
                .sendEmail(true)
                .emailTemplate(TEMPLATE_USAGE_ALERT)
                .emailPlaceholders(placeholders)
                .build();
        notificationService.create(params);
    }

    private static String trimLower(String value) {
        return value == null ? null : value.trim().toLowerCase(Locale.ROOT);
    }

    private static void validate(BizContext context) {
        if (context == null || !StringUtils.hasText(context.getTenantId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "tenantId is required");
        }
    }
}
