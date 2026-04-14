package com.sme.be_sme.modules.platform.processor.dashboard;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.billing.infrastructure.mapper.PlanMapper;
import com.sme.be_sme.modules.billing.infrastructure.mapper.SubscriptionMapper;
import com.sme.be_sme.modules.billing.infrastructure.persistence.entity.PlanEntity;
import com.sme.be_sme.modules.billing.infrastructure.persistence.entity.SubscriptionEntity;
import com.sme.be_sme.modules.company.infrastructure.mapper.CompanyMapper;
import com.sme.be_sme.modules.company.infrastructure.persistence.entity.CompanyEntity;
import com.sme.be_sme.modules.identity.infrastructure.mapper.UserMapper;
import com.sme.be_sme.modules.identity.infrastructure.persistence.entity.UserEntity;
import com.sme.be_sme.modules.onboarding.infrastructure.mapper.OnboardingInstanceMapper;
import com.sme.be_sme.modules.onboarding.infrastructure.persistence.entity.OnboardingInstanceEntity;
import com.sme.be_sme.modules.platform.api.request.PlatformDashboardOverviewRequest;
import com.sme.be_sme.modules.platform.api.response.PlatformDashboardOverviewResponse;
import com.sme.be_sme.modules.platform.processor.analytics.PlatformAnalyticsSupport;
import com.sme.be_sme.shared.gateway.core.BaseBizProcessor;
import com.sme.be_sme.shared.gateway.core.BizContext;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PlatformDashboardOverviewProcessor extends BaseBizProcessor<BizContext> {

    private final ObjectMapper objectMapper;
    private final CompanyMapper companyMapper;
    private final SubscriptionMapper subscriptionMapper;
    private final PlanMapper planMapper;
    private final OnboardingInstanceMapper onboardingInstanceMapper;
    private final UserMapper userMapper;

    @Override
    protected Object doProcess(BizContext context, JsonNode payload) {
        PlatformDashboardOverviewRequest request = objectMapper.convertValue(payload, PlatformDashboardOverviewRequest.class);
        String groupBy = PlatformAnalyticsSupport.normalizeGroupBy(request.getGroupBy());
        LocalDate start = PlatformAnalyticsSupport.parseLocalDate(request.getStartDate(), LocalDate.now().minusMonths(1));
        LocalDate end = PlatformAnalyticsSupport.parseLocalDate(request.getEndDate(), LocalDate.now());
        LocalDate previousStart = PlatformAnalyticsSupport.previousPeriodStart(start, end, groupBy);
        LocalDate previousEnd = PlatformAnalyticsSupport.previousPeriodEnd(start, end, groupBy);

        DateRange current = new DateRange(request.getStartDate(), request.getEndDate());
        DateRange previous = new DateRange(previousStart.toString(), previousEnd.toString());

        List<CompanyEntity> companies = companyMapper.selectAll();
        List<SubscriptionEntity> subscriptions = subscriptionMapper.selectAll();
        List<PlanEntity> plans = planMapper.selectAll();
        List<OnboardingInstanceEntity> onboardings = onboardingInstanceMapper.selectAll();
        List<UserEntity> users = userMapper.selectAll();

        Map<String, PlanEntity> plansById = new HashMap<>();
        for (PlanEntity plan : plans) {
            if (plan != null && plan.getPlanId() != null) {
                plansById.put(plan.getPlanId(), plan);
            }
        }

        int totalCompanies = countCompanies(companies, current.start, current.end);
        int previousCompanies = countCompanies(companies, previous.start, previous.end);
        double mrr = calculateMrr(subscriptions, plansById, current.end);
        double previousMrr = calculateMrr(subscriptions, plansById, previous.end);
        int activeOnboardings = countActiveOnboardings(onboardings, current.start, current.end);
        int previousActiveOnboardings = countActiveOnboardings(onboardings, previous.start, previous.end);
        int riskOnboardings = countRiskOnboardings(onboardings, current.start, current.end);
        int previousRiskOnboardings = countRiskOnboardings(onboardings, previous.start, previous.end);
        int totalEmployees = countEmployees(users, current.start, current.end);
        int previousEmployees = countEmployees(users, previous.start, previous.end);

        PlatformDashboardOverviewResponse response = new PlatformDashboardOverviewResponse();
        response.setStartDate(start.toString());
        response.setEndDate(end.toString());
        response.setGroupBy(groupBy);
        response.setTotalCompanies(totalCompanies);
        response.setCompanyGrowthRate(PlatformAnalyticsSupport.growth(totalCompanies, previousCompanies));
        response.setMrr(mrr);
        response.setMrrGrowthRate(PlatformAnalyticsSupport.growth(mrr, previousMrr));
        response.setActiveOnboardings(activeOnboardings);
        response.setActiveOnboardingsGrowthRate(PlatformAnalyticsSupport.growth(activeOnboardings, previousActiveOnboardings));
        response.setRiskOnboardings(riskOnboardings);
        response.setRiskOnboardingsGrowthRate(PlatformAnalyticsSupport.growth(riskOnboardings, previousRiskOnboardings));
        response.setTotalEmployees(totalEmployees);
        response.setEmployeeGrowthRate(PlatformAnalyticsSupport.growth(totalEmployees, previousEmployees));
        return response;
    }

    private int countCompanies(List<CompanyEntity> companies, java.util.Date start, java.util.Date end) {
        int count = 0;
        for (CompanyEntity company : companies) {
            if (company != null && !"PLATFORM".equalsIgnoreCase(company.getStatus())
                    && PlatformAnalyticsSupport.inRange(company.getCreatedAt(), start, end)) {
                count++;
            }
        }
        return count;
    }

    private double calculateMrr(List<SubscriptionEntity> subscriptions, Map<String, PlanEntity> plansById, java.util.Date endExclusive) {
        double total = 0.0;
        for (SubscriptionEntity sub : subscriptions) {
            if (sub == null || !"ACTIVE".equalsIgnoreCase(sub.getStatus())) {
                continue;
            }
            if (endExclusive != null && sub.getCreatedAt() != null && !sub.getCreatedAt().before(endExclusive)) {
                continue;
            }
            PlanEntity plan = plansById.get(sub.getPlanId());
            if (plan == null) {
                continue;
            }
            if ("YEARLY".equalsIgnoreCase(sub.getBillingCycle())) {
                total += plan.getPriceVndYearly() != null ? plan.getPriceVndYearly() / 12.0 : 0.0;
            } else {
                total += plan.getPriceVndMonthly() != null ? plan.getPriceVndMonthly() : 0.0;
            }
        }
        return total;
    }

    private int countActiveOnboardings(List<OnboardingInstanceEntity> onboardings, java.util.Date start, java.util.Date end) {
        int count = 0;
        for (OnboardingInstanceEntity item : onboardings) {
            if (item == null || !PlatformAnalyticsSupport.inRange(item.getCreatedAt(), start, end)) {
                continue;
            }
            if (!"COMPLETED".equalsIgnoreCase(item.getStatus()) && !"CANCELLED".equalsIgnoreCase(item.getStatus())) {
                count++;
            }
        }
        return count;
    }

    private int countRiskOnboardings(List<OnboardingInstanceEntity> onboardings, java.util.Date start, java.util.Date end) {
        int count = 0;
        for (OnboardingInstanceEntity item : onboardings) {
            if (item == null || !PlatformAnalyticsSupport.inRange(item.getCreatedAt(), start, end)) {
                continue;
            }
            if ("OVERDUE".equalsIgnoreCase(item.getStatus()) || "RISK".equalsIgnoreCase(item.getStatus())) {
                count++;
            }
        }
        return count;
    }

    private int countEmployees(List<UserEntity> users, java.util.Date start, java.util.Date end) {
        int count = 0;
        for (UserEntity user : users) {
            if (user == null) {
                continue;
            }

            if (!PlatformAnalyticsSupport.inRange(user.getCreatedAt(), start, end)) {
                continue;
            }

            if (isAdminUser(user)) {
                continue;
            }

            if (PlatformAnalyticsSupport.isEmployee(user)) {
                count++;
            }
        }
        return count;
    }

    private boolean isAdminUser(UserEntity user) {
        return hasRole(user, "ADMIN")
                || hasRole(user, "PLATFORM_ADMIN")
                || hasRole(user, "HR_ADMIN")
                || hasRole(user, "SUPER_ADMIN");
    }

    private boolean hasRole(UserEntity user, String roleName) {
        if (user == null || roleName == null) {
            return false;
        }

        try {
            Object rolesObj = null;

            try {
                rolesObj = user.getClass().getMethod("getRole").invoke(user);
            } catch (Exception ignored) {
            }

            if (rolesObj == null) {
                try {
                    rolesObj = user.getClass().getMethod("getRoles").invoke(user);
                } catch (Exception ignored) {
                }
            }

            if (rolesObj == null) {
                try {
                    rolesObj = user.getClass().getMethod("getUserType").invoke(user);
                } catch (Exception ignored) {
                }
            }

            if (rolesObj == null) {
                return false;
            }

            String normalized = String.valueOf(rolesObj).toUpperCase();
            return normalized.contains(roleName.toUpperCase());
        } catch (Exception e) {
            return false;
        }
    }

    private static class DateRange {
        private final java.util.Date start;
        private final java.util.Date end;
        private DateRange(String startDate, String endDate) {
            this.start = PlatformAnalyticsSupport.parseDate(startDate, true);
            this.end = PlatformAnalyticsSupport.parseDate(endDate, false);
        }
    }
}
