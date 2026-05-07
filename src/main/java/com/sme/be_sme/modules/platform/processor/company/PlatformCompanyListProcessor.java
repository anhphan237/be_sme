package com.sme.be_sme.modules.platform.processor.company;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.billing.infrastructure.mapper.PlanMapper;
import com.sme.be_sme.modules.billing.infrastructure.mapper.SubscriptionMapper;
import com.sme.be_sme.modules.billing.infrastructure.persistence.entity.PlanEntity;
import com.sme.be_sme.modules.billing.infrastructure.persistence.entity.SubscriptionEntity;
import com.sme.be_sme.modules.company.infrastructure.mapper.CompanyMapper;
import com.sme.be_sme.modules.company.infrastructure.persistence.entity.CompanyEntity;
import com.sme.be_sme.modules.platform.api.request.PlatformCompanyListRequest;
import com.sme.be_sme.modules.platform.api.response.PlatformCompanyListResponse;
import com.sme.be_sme.modules.platform.api.response.PlatformCompanyListResponse.CompanyItem;
import com.sme.be_sme.shared.gateway.core.BaseBizProcessor;
import com.sme.be_sme.shared.gateway.core.BizContext;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class PlatformCompanyListProcessor extends BaseBizProcessor<BizContext> {

    private static final String PLATFORM_STATUS = "PLATFORM";
    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_SIZE = 20;

    private static final BigDecimal BYTES_PER_MB = BigDecimal.valueOf(1024L * 1024L);
    private static final BigDecimal BYTES_PER_GB = BigDecimal.valueOf(1024L * 1024L * 1024L);

    private final ObjectMapper objectMapper;
    private final CompanyMapper companyMapper;
    private final SubscriptionMapper subscriptionMapper;
    private final PlanMapper planMapper;
    private final JdbcTemplate jdbcTemplate;

    @Override
    protected Object doProcess(BizContext context, JsonNode payload) {
        PlatformCompanyListRequest request =
                objectMapper.convertValue(payload, PlatformCompanyListRequest.class);

        int page = request.getPage() != null && request.getPage() > 0
                ? request.getPage()
                : DEFAULT_PAGE;

        int size = request.getSize() != null && request.getSize() > 0
                ? request.getSize()
                : DEFAULT_SIZE;

        String currentMonth = YearMonth.now().toString();

        List<CompanyEntity> allCompanies = companyMapper.selectAll();
        List<SubscriptionEntity> allSubscriptions = subscriptionMapper.selectAll();

        Map<String, PlanEntity> plansById = buildPlanMap();
        Map<String, SubscriptionEntity> subscriptionByCompany =
                buildCurrentSubscriptionByCompany(allSubscriptions);

        Map<String, UsageSnapshot> usageByCompany = buildUsageSnapshot(currentMonth);

        List<CompanyEntity> filtered = new ArrayList<>();
        for (CompanyEntity company : allCompanies) {
            if (company == null) {
                continue;
            }

            if (PLATFORM_STATUS.equalsIgnoreCase(String.valueOf(company.getStatus()))) {
                continue;
            }

            if (StringUtils.hasText(request.getStatus())
                    && !request.getStatus().trim()
                    .equalsIgnoreCase(String.valueOf(company.getStatus()).trim())) {
                continue;
            }

            if (StringUtils.hasText(request.getSearch())
                    && (company.getName() == null
                    || !company.getName().toLowerCase()
                    .contains(request.getSearch().trim().toLowerCase()))) {
                continue;
            }

            if (StringUtils.hasText(request.getPlanCode())) {
                SubscriptionEntity sub = subscriptionByCompany.get(company.getCompanyId());

                if (sub == null) {
                    continue;
                }

                PlanEntity plan = sub.getPlanId() != null ? plansById.get(sub.getPlanId()) : null;

                if (plan == null
                        || !request.getPlanCode().trim()
                        .equalsIgnoreCase(String.valueOf(plan.getCode()).trim())) {
                    continue;
                }
            }

            filtered.add(company);
        }

        int total = filtered.size();
        int fromIndex = Math.min((page - 1) * size, total);
        int toIndex = Math.min(fromIndex + size, total);
        List<CompanyEntity> pageSlice = filtered.subList(fromIndex, toIndex);

        List<CompanyItem> items = new ArrayList<>();

        for (CompanyEntity company : pageSlice) {
            String companyId = company.getCompanyId();
            UsageSnapshot usage = usageByCompany.getOrDefault(companyId, UsageSnapshot.empty());

            CompanyItem item = new CompanyItem();
            item.setCompanyId(companyId);
            item.setName(company.getName());
            item.setStatus(company.getStatus());
            item.setCreatedAt(company.getCreatedAt());

            item.setUserCount(usage.userCount);
            item.setOnboardedThisMonth(usage.onboardedThisMonth);
            item.setActiveOnboardingCount(usage.activeOnboardingCount);
            item.setOnboardingTemplateCount(usage.onboardingTemplateCount);
            item.setEventTemplateCount(usage.eventTemplateCount);
            item.setDocumentCount(usage.documentCount);

            item.setStorageUsedBytes(usage.storageUsedBytes);
            item.setStorageUsedMb(toMb(usage.storageUsedBytes));
            item.setStorageUsedGb(toGb(usage.storageUsedBytes));

            SubscriptionEntity sub = subscriptionByCompany.get(companyId);
            PlanEntity plan = null;

            if (sub != null) {
                item.setSubscriptionId(sub.getSubscriptionId());
                item.setSubscriptionStatus(sub.getStatus());
                item.setBillingCycle(sub.getBillingCycle());

                if (sub.getPlanId() != null) {
                    plan = plansById.get(sub.getPlanId());
                }
            }

            if (plan != null) {
                item.setPlanId(plan.getPlanId());
                item.setPlanCode(plan.getCode());
                item.setPlanName(plan.getName());
                item.setPlanStatus(plan.getStatus());

                applyPlanLimit(item, plan);
            } else {
                item.setUsageLevel("NO_PLAN");
                item.setOverallUsagePercent(BigDecimal.ZERO);
            }

            items.add(item);
        }

        PlatformCompanyListResponse response = new PlatformCompanyListResponse();
        response.setItems(items);
        response.setTotal(total);
        return response;
    }

    private void applyPlanLimit(CompanyItem item, PlanEntity plan) {
        item.setEmployeeLimitPerMonth(plan.getEmployeeLimitPerMonth());
        item.setOnboardingTemplateLimit(plan.getOnboardingTemplateLimit());
        item.setEventTemplateLimit(plan.getEventTemplateLimit());
        item.setDocumentLimit(plan.getDocumentLimit());

        item.setStorageLimitBytes(plan.getStorageLimitBytes());
        item.setStorageLimitMb(toMb(plan.getStorageLimitBytes()));
        item.setStorageLimitGb(toGb(plan.getStorageLimitBytes()));

        BigDecimal employeePercent = percent(item.getOnboardedThisMonth(), plan.getEmployeeLimitPerMonth());
        BigDecimal onboardingTemplatePercent =
                percent(item.getOnboardingTemplateCount(), plan.getOnboardingTemplateLimit());
        BigDecimal eventTemplatePercent =
                percent(item.getEventTemplateCount(), plan.getEventTemplateLimit());
        BigDecimal documentPercent =
                percent(item.getDocumentCount(), plan.getDocumentLimit());
        BigDecimal storagePercent =
                percent(item.getStorageUsedBytes(), plan.getStorageLimitBytes());

        item.setEmployeeUsagePercent(employeePercent);
        item.setOnboardingTemplateUsagePercent(onboardingTemplatePercent);
        item.setEventTemplateUsagePercent(eventTemplatePercent);
        item.setDocumentUsagePercent(documentPercent);
        item.setStorageUsagePercent(storagePercent);

        BigDecimal overall = maxPercent(
                employeePercent,
                onboardingTemplatePercent,
                eventTemplatePercent,
                documentPercent,
                storagePercent
        );

        item.setOverallUsagePercent(overall);
        item.setUsageLevel(resolveUsageLevel(overall));
    }

    private Map<String, PlanEntity> buildPlanMap() {
        Map<String, PlanEntity> map = new HashMap<>();

        for (PlanEntity plan : planMapper.selectAll()) {
            if (plan != null && plan.getPlanId() != null) {
                map.put(plan.getPlanId(), plan);
            }
        }

        return map;
    }

    private Map<String, SubscriptionEntity> buildCurrentSubscriptionByCompany(
            List<SubscriptionEntity> subscriptions
    ) {
        Map<String, SubscriptionEntity> result = new HashMap<>();

        for (SubscriptionEntity sub : subscriptions) {
            if (sub == null || sub.getCompanyId() == null) {
                continue;
            }

            SubscriptionEntity current = result.get(sub.getCompanyId());

            if (current == null || isBetterSubscription(sub, current)) {
                result.put(sub.getCompanyId(), sub);
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

    private Map<String, UsageSnapshot> buildUsageSnapshot(String currentMonth) {
        Map<String, UsageSnapshot> map = new HashMap<>();

        mergeInteger(
                map,
                "select company_id, count(*) as value " +
                        "from users " +
                        "where company_id is not null " +
                        "group by company_id",
                "userCount"
        );

        mergeInteger(
                map,
                "select company_id, coalesce(sum(onboarded_employee_count), 0) as value " +
                        "from usage_monthly " +
                        "where month = ? and company_id is not null " +
                        "group by company_id",
                "onboardedThisMonth",
                currentMonth
        );

        mergeInteger(
                map,
                "select company_id, count(*) as value " +
                        "from onboarding_instances " +
                        "where company_id is not null " +
                        "and upper(coalesce(status, '')) not in ('COMPLETED', 'CANCELLED', 'CANCELED', 'CLOSED', 'DELETED') " +
                        "group by company_id",
                "activeOnboardingCount"
        );

        mergeInteger(
                map,
                "select company_id, count(*) as value " +
                        "from onboarding_templates " +
                        "where company_id is not null " +
                        "and upper(coalesce(status, 'ACTIVE')) = 'ACTIVE' " +
                        "and upper(coalesce(template_kind, 'ONBOARDING')) = 'ONBOARDING' " +
                        "group by company_id",
                "onboardingTemplateCount"
        );

        mergeInteger(
                map,
                "select company_id, count(*) as value " +
                        "from event_templates " +
                        "where company_id is not null " +
                        "and upper(coalesce(status, 'ACTIVE')) = 'ACTIVE' " +
                        "group by company_id",
                "eventTemplateCount"
        );

        mergeInteger(
                map,
                "select company_id, count(*) as value " +
                        "from documents " +
                        "where company_id is not null " +
                        "and upper(coalesce(status, 'ACTIVE')) not in ('DELETED') " +
                        "group by company_id",
                "documentCount"
        );

        mergeLong(
                map,
                "select company_id, coalesce(sum(file_size_bytes), 0) as value " +
                        "from document_versions " +
                        "where company_id is not null " +
                        "group by company_id",
                "storageUsedBytes"
        );

        mergeLong(
                map,
                "select company_id, coalesce(sum(file_size_bytes), 0) as value " +
                        "from document_attachments " +
                        "where company_id is not null " +
                        "and upper(coalesce(status, 'ACTIVE')) = 'ACTIVE' " +
                        "group by company_id",
                "storageUsedBytes"
        );

        mergeLong(
                map,
                "select company_id, coalesce(sum(file_size_bytes), 0) as value " +
                        "from task_attachments " +
                        "where company_id is not null " +
                        "group by company_id",
                "storageUsedBytes"
        );

        return map;
    }

    private void mergeInteger(
            Map<String, UsageSnapshot> map,
            String sql,
            String field,
            Object... args
    ) {
        jdbcTemplate.query(sql, args, rs -> {
            String companyId = rs.getString("company_id");
            int value = rs.getInt("value");

            UsageSnapshot snapshot = map.computeIfAbsent(companyId, key -> UsageSnapshot.empty());
            snapshot.setInteger(field, value);
        });
    }

    private void mergeLong(
            Map<String, UsageSnapshot> map,
            String sql,
            String field,
            Object... args
    ) {
        jdbcTemplate.query(sql, args, rs -> {
            String companyId = rs.getString("company_id");
            long value = rs.getLong("value");

            UsageSnapshot snapshot = map.computeIfAbsent(companyId, key -> UsageSnapshot.empty());
            snapshot.addLong(field, value);
        });
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

        static UsageSnapshot empty() {
            return new UsageSnapshot();
        }

        void setInteger(String field, Integer value) {
            if ("userCount".equals(field)) {
                userCount = value;
            } else if ("onboardedThisMonth".equals(field)) {
                onboardedThisMonth = value;
            } else if ("activeOnboardingCount".equals(field)) {
                activeOnboardingCount = value;
            } else if ("onboardingTemplateCount".equals(field)) {
                onboardingTemplateCount = value;
            } else if ("eventTemplateCount".equals(field)) {
                eventTemplateCount = value;
            } else if ("documentCount".equals(field)) {
                documentCount = value;
            }
        }

        void addLong(String field, Long value) {
            if ("storageUsedBytes".equals(field)) {
                storageUsedBytes += value == null ? 0L : value;
            }
        }
    }
}