package com.sme.be_sme.modules.platform.processor.company;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.billing.infrastructure.mapper.PlanMapper;
import com.sme.be_sme.modules.billing.infrastructure.mapper.SubscriptionMapper;
import com.sme.be_sme.modules.billing.infrastructure.persistence.entity.PlanEntity;
import com.sme.be_sme.modules.billing.infrastructure.persistence.entity.SubscriptionEntity;
import com.sme.be_sme.modules.company.infrastructure.mapper.CompanyMapper;
import com.sme.be_sme.modules.company.infrastructure.persistence.entity.CompanyEntity;
import com.sme.be_sme.modules.platform.api.request.PlatformCompanyDetailRequest;
import com.sme.be_sme.modules.platform.api.response.PlatformCompanyDetailResponse;
import com.sme.be_sme.shared.constant.ErrorCodes;
import com.sme.be_sme.shared.exception.AppException;
import com.sme.be_sme.shared.gateway.core.BaseBizProcessor;
import com.sme.be_sme.shared.gateway.core.BizContext;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.Date;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class PlatformCompanyDetailProcessor extends BaseBizProcessor<BizContext> {

    private static final BigDecimal BYTES_PER_MB = BigDecimal.valueOf(1024L * 1024L);
    private static final BigDecimal BYTES_PER_GB = BigDecimal.valueOf(1024L * 1024L * 1024L);

    private final ObjectMapper objectMapper;
    private final CompanyMapper companyMapper;
    private final SubscriptionMapper subscriptionMapper;
    private final PlanMapper planMapper;
    private final JdbcTemplate jdbcTemplate;

    @Override
    protected Object doProcess(BizContext context, JsonNode payload) {
        PlatformCompanyDetailRequest request =
                objectMapper.convertValue(payload, PlatformCompanyDetailRequest.class);

        if (!StringUtils.hasText(request.getCompanyId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "companyId is required");
        }

        String companyId = request.getCompanyId();

        CompanyEntity company = companyMapper.selectByPrimaryKey(companyId);
        if (company == null) {
            throw AppException.of(ErrorCodes.NOT_FOUND, "Company not found");
        }

        UsageSnapshot usage = buildUsageSnapshot(companyId, YearMonth.now().toString());

        SubscriptionEntity subscription = findCurrentSubscriptionByCompany(companyId);

        PlanEntity plan = null;
        if (subscription != null && subscription.getPlanId() != null) {
            plan = planMapper.selectByPrimaryKey(subscription.getPlanId());
        }

        PlatformCompanyDetailResponse response = new PlatformCompanyDetailResponse();
        response.setCompanyId(company.getCompanyId());
        response.setName(company.getName());
        response.setTaxCode(company.getTaxCode());
        response.setAddress(company.getAddress());
        response.setStatus(company.getStatus());
        response.setCreatedAt(company.getCreatedAt());

        response.setUserCount(usage.userCount);
        response.setOnboardedThisMonth(usage.onboardedThisMonth);
        response.setActiveOnboardingCount(usage.activeOnboardingCount);
        response.setOnboardingTemplateCount(usage.onboardingTemplateCount);
        response.setEventTemplateCount(usage.eventTemplateCount);
        response.setDocumentCount(usage.documentCount);

        response.setStorageUsedBytes(usage.storageUsedBytes);
        response.setStorageUsedMb(toMb(usage.storageUsedBytes));
        response.setStorageUsedGb(toGb(usage.storageUsedBytes));

        if (subscription != null) {
            response.setSubscriptionId(subscription.getSubscriptionId());
            response.setSubscriptionStatus(subscription.getStatus());
            response.setBillingCycle(subscription.getBillingCycle());
            response.setCurrentPeriodEnd(subscription.getCurrentPeriodEnd());
        }

        if (plan != null) {
            response.setPlanId(plan.getPlanId());
            response.setPlanCode(plan.getCode());
            response.setPlanName(plan.getName());
            response.setPlanStatus(plan.getStatus());

            applyPlanLimit(response, plan);
        } else {
            response.setUsageLevel("NO_PLAN");
            response.setOverallUsagePercent(BigDecimal.ZERO);
        }

        return response;
    }

    private void applyPlanLimit(PlatformCompanyDetailResponse response, PlanEntity plan) {
        response.setEmployeeLimitPerMonth(plan.getEmployeeLimitPerMonth());
        response.setOnboardingTemplateLimit(plan.getOnboardingTemplateLimit());
        response.setEventTemplateLimit(plan.getEventTemplateLimit());
        response.setDocumentLimit(plan.getDocumentLimit());

        response.setStorageLimitBytes(plan.getStorageLimitBytes());
        response.setStorageLimitMb(toMb(plan.getStorageLimitBytes()));
        response.setStorageLimitGb(toGb(plan.getStorageLimitBytes()));

        BigDecimal employeePercent =
                percent(response.getOnboardedThisMonth(), plan.getEmployeeLimitPerMonth());

        BigDecimal onboardingTemplatePercent =
                percent(response.getOnboardingTemplateCount(), plan.getOnboardingTemplateLimit());

        BigDecimal eventTemplatePercent =
                percent(response.getEventTemplateCount(), plan.getEventTemplateLimit());

        BigDecimal documentPercent =
                percent(response.getDocumentCount(), plan.getDocumentLimit());

        BigDecimal storagePercent =
                percent(response.getStorageUsedBytes(), plan.getStorageLimitBytes());

        response.setEmployeeUsagePercent(employeePercent);
        response.setOnboardingTemplateUsagePercent(onboardingTemplatePercent);
        response.setEventTemplateUsagePercent(eventTemplatePercent);
        response.setDocumentUsagePercent(documentPercent);
        response.setStorageUsagePercent(storagePercent);

        BigDecimal overall = maxPercent(
                employeePercent,
                onboardingTemplatePercent,
                eventTemplatePercent,
                documentPercent,
                storagePercent
        );

        response.setOverallUsagePercent(overall);
        response.setUsageLevel(resolveUsageLevel(overall));
    }

    private SubscriptionEntity findCurrentSubscriptionByCompany(String companyId) {
        List<SubscriptionEntity> all = subscriptionMapper.selectAll();

        SubscriptionEntity result = null;

        for (SubscriptionEntity sub : all) {
            if (sub == null || !companyId.equals(sub.getCompanyId())) {
                continue;
            }

            if (result == null || isBetterSubscription(sub, result)) {
                result = sub;
            }
        }

        return result;
    }

    private boolean isBetterSubscription(SubscriptionEntity candidate, SubscriptionEntity current) {
        boolean candidateActive = "ACTIVE".equalsIgnoreCase(candidate.getStatus());
        boolean currentActive = "ACTIVE".equalsIgnoreCase(current.getStatus());

        if (candidateActive && !currentActive) {
            return true;
        }

        if (!candidateActive && currentActive) {
            return false;
        }

        Date candidateDate = candidate.getUpdatedAt() != null
                ? candidate.getUpdatedAt()
                : candidate.getCreatedAt();

        Date currentDate = current.getUpdatedAt() != null
                ? current.getUpdatedAt()
                : current.getCreatedAt();

        if (candidateDate == null) {
            return false;
        }

        if (currentDate == null) {
            return true;
        }

        return candidateDate.after(currentDate);
    }

    private UsageSnapshot buildUsageSnapshot(String companyId, String currentMonth) {
        UsageSnapshot snapshot = new UsageSnapshot();

        snapshot.userCount = queryInt(
                "select count(*) " +
                        "from users " +
                        "where company_id = ?",
                companyId
        );

        snapshot.onboardedThisMonth = queryInt(
                "select coalesce(sum(onboarded_employee_count), 0) " +
                        "from usage_monthly " +
                        "where company_id = ? and month = ?",
                companyId,
                currentMonth
        );

        snapshot.activeOnboardingCount = queryInt(
                "select count(*) " +
                        "from onboarding_instances " +
                        "where company_id = ? " +
                        "and upper(coalesce(status, '')) not in ('COMPLETED', 'CANCELLED', 'CANCELED', 'CLOSED', 'DELETED')",
                companyId
        );

        snapshot.onboardingTemplateCount = queryInt(
                "select count(*) " +
                        "from onboarding_templates " +
                        "where company_id = ? " +
                        "and upper(coalesce(status, 'ACTIVE')) = 'ACTIVE' " +
                        "and upper(coalesce(template_kind, 'ONBOARDING')) = 'ONBOARDING'",
                companyId
        );

        snapshot.eventTemplateCount = queryInt(
                "select count(*) " +
                        "from event_templates " +
                        "where company_id = ? " +
                        "and upper(coalesce(status, 'ACTIVE')) = 'ACTIVE'",
                companyId
        );

        snapshot.documentCount = queryInt(
                "select count(*) " +
                        "from documents " +
                        "where company_id = ? " +
                        "and upper(coalesce(status, 'ACTIVE')) not in ('DELETED')",
                companyId
        );

        long documentVersionBytes = queryLong(
                "select coalesce(sum(file_size_bytes), 0) " +
                        "from document_versions " +
                        "where company_id = ?",
                companyId
        );

        long documentAttachmentBytes = queryLong(
                "select coalesce(sum(file_size_bytes), 0) " +
                        "from document_attachments " +
                        "where company_id = ? " +
                        "and upper(coalesce(status, 'ACTIVE')) = 'ACTIVE'",
                companyId
        );

        long taskAttachmentBytes = queryLong(
                "select coalesce(sum(file_size_bytes), 0) " +
                        "from task_attachments " +
                        "where company_id = ?",
                companyId
        );

        snapshot.storageUsedBytes =
                documentVersionBytes + documentAttachmentBytes + taskAttachmentBytes;

        return snapshot;
    }

    private int queryInt(String sql, Object... args) {
        Integer value = jdbcTemplate.queryForObject(sql, Integer.class, args);
        return value == null ? 0 : value;
    }

    private long queryLong(String sql, Object... args) {
        Long value = jdbcTemplate.queryForObject(sql, Long.class, args);
        return value == null ? 0L : value;
    }

    private BigDecimal percent(Integer used, Integer limit) {
        if (used == null || limit == null || limit <= 0) {
            return null;
        }

        return percent(BigDecimal.valueOf(used), BigDecimal.valueOf(limit));
    }

    private BigDecimal percent(Long used, Long limit) {
        if (used == null || limit == null || limit <= 0) {
            return null;
        }

        return percent(BigDecimal.valueOf(used), BigDecimal.valueOf(limit));
    }

    private BigDecimal percent(BigDecimal used, BigDecimal limit) {
        if (limit == null || BigDecimal.ZERO.compareTo(limit) >= 0) {
            return null;
        }

        return used
                .multiply(BigDecimal.valueOf(100))
                .divide(limit, 2, RoundingMode.HALF_UP);
    }

    private BigDecimal maxPercent(BigDecimal... values) {
        BigDecimal max = BigDecimal.ZERO;

        for (BigDecimal value : values) {
            if (value != null && value.compareTo(max) > 0) {
                max = value;
            }
        }

        return max;
    }

    private String resolveUsageLevel(BigDecimal percent) {
        if (percent == null || percent.compareTo(BigDecimal.ZERO) <= 0) {
            return "NONE";
        }

        if (percent.compareTo(BigDecimal.valueOf(100)) > 0) {
            return "OVER_LIMIT";
        }

        if (percent.compareTo(BigDecimal.valueOf(80)) >= 0) {
            return "HIGH";
        }

        if (percent.compareTo(BigDecimal.valueOf(50)) >= 0) {
            return "MEDIUM";
        }

        return "LOW";
    }

    private BigDecimal toMb(Long bytes) {
        if (bytes == null) {
            return null;
        }

        return BigDecimal.valueOf(bytes).divide(BYTES_PER_MB, 2, RoundingMode.HALF_UP);
    }

    private BigDecimal toGb(Long bytes) {
        if (bytes == null) {
            return null;
        }

        return BigDecimal.valueOf(bytes).divide(BYTES_PER_GB, 2, RoundingMode.HALF_UP);
    }

    private static class UsageSnapshot {
        private Integer userCount = 0;
        private Integer onboardedThisMonth = 0;
        private Integer activeOnboardingCount = 0;
        private Integer onboardingTemplateCount = 0;
        private Integer eventTemplateCount = 0;
        private Integer documentCount = 0;
        private Long storageUsedBytes = 0L;
    }
}