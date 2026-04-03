package com.sme.be_sme.modules.billing.job;

import com.sme.be_sme.modules.billing.infrastructure.mapper.OnboardingUsageMapper;
import com.sme.be_sme.modules.billing.infrastructure.mapper.PlanMapper;
import com.sme.be_sme.modules.billing.infrastructure.mapper.SubscriptionMapper;
import com.sme.be_sme.modules.identity.infrastructure.mapper.UserRoleMapperExt;
import com.sme.be_sme.modules.billing.infrastructure.persistence.entity.PlanEntity;
import com.sme.be_sme.modules.billing.infrastructure.persistence.entity.SubscriptionEntity;
import com.sme.be_sme.modules.notification.service.NotificationCreateParams;
import com.sme.be_sme.modules.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Daily job: warn when usage approaches (>=80%) or exceeds plan limit. Notifies ADMIN users.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class QuotaWarningJob {

    private static final double APPROACHING_THRESHOLD = 80.0;
    private static final double EXCEEDED_THRESHOLD = 100.0;
    private static final String TEMPLATE_QUOTA_WARNING = "QUOTA_WARNING";

    private final OnboardingUsageMapper onboardingUsageMapper;
    private final SubscriptionMapper subscriptionMapper;
    private final PlanMapper planMapper;
    private final UserRoleMapperExt userRoleMapperExt;
    private final NotificationService notificationService;

    @Scheduled(cron = "${app.billing.quota-warning.cron:0 0 10 * * ?}")
    public void run() {
        List<SubscriptionEntity> active = subscriptionMapper.selectAll().stream()
                .filter(Objects::nonNull)
                .filter(s -> "ACTIVE".equalsIgnoreCase(trimLower(s.getStatus())))
                .toList();
        if (active.isEmpty()) return;
        YearMonth ym = YearMonth.now();
        String month = ym.format(DateTimeFormatter.ofPattern("yyyy-MM"));
        Date monthStart = Date.from(ym.atDay(1).atStartOfDay(ZoneId.systemDefault()).toInstant());
        Calendar cal = Calendar.getInstance(Locale.US);
        cal.setTime(monthStart);
        cal.add(Calendar.MONTH, 1);
        Date monthEnd = cal.getTime();

        for (SubscriptionEntity sub : active) {
            try {
                processSubscription(sub, month, monthStart, monthEnd);
            } catch (Exception e) {
                log.warn("QuotaWarningJob: failed for {}: {}", sub.getSubscriptionId(), e.getMessage());
            }
        }
    }

    private void processSubscription(SubscriptionEntity sub, String month, Date monthStart, Date monthEnd) {
        String companyId = sub.getCompanyId();
        PlanEntity plan = planMapper.selectByPrimaryKey(sub.getPlanId());
        if (plan == null || plan.getEmployeeLimitPerMonth() == null || plan.getEmployeeLimitPerMonth() <= 0) return;
        int limit = plan.getEmployeeLimitPerMonth();
        int count = onboardingUsageMapper.countOnboardingInstancesByCompanyAndDateRange(companyId, monthStart, monthEnd);
        int percent = limit == 0 ? 0 : (int) Math.round(100.0 * count / limit);
        if (percent < APPROACHING_THRESHOLD) return;

        List<String> adminUserIds = userRoleMapperExt.selectUserIdsByCompanyAndRoleCode(companyId, "HR");
        if (adminUserIds == null || adminUserIds.isEmpty()) return;

        String message = percent >= EXCEEDED_THRESHOLD ? "Limit exceeded." : "Approaching limit.";
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("month", month);
        placeholders.put("usage", String.valueOf(count));
        placeholders.put("limit", String.valueOf(limit));
        placeholders.put("percent", String.valueOf(percent));
        placeholders.put("message", message);

        String title = percent >= EXCEEDED_THRESHOLD ? "Usage limit exceeded" : "Usage approaching limit";
        String content = String.format("Onboarding usage for %s: %d of %d (%d%%). %s", month, count, limit, percent, message);

        for (String userId : adminUserIds) {
            if (!StringUtils.hasText(userId)) continue;
            NotificationCreateParams params = NotificationCreateParams.builder()
                    .companyId(companyId)
                    .userId(userId)
                    .type("QUOTA_WARNING")
                    .title(title)
                    .content(content)
                    .refType("USAGE")
                    .refId(month)
                    .sendEmail(true)
                    .emailTemplate(TEMPLATE_QUOTA_WARNING)
                    .emailPlaceholders(placeholders)
                    .build();
            notificationService.create(params);
        }
    }

    private static String trimLower(String value) {
        return value == null ? null : value.trim().toLowerCase(Locale.ROOT);
    }
}
