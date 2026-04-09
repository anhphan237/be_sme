package com.sme.be_sme.modules.platform.processor.dashboard;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.billing.infrastructure.mapper.PaymentTransactionMapper;
import com.sme.be_sme.modules.billing.infrastructure.mapper.PlanMapper;
import com.sme.be_sme.modules.billing.infrastructure.mapper.SubscriptionMapper;
import com.sme.be_sme.modules.billing.infrastructure.persistence.entity.PaymentTransactionEntity;
import com.sme.be_sme.modules.billing.infrastructure.persistence.entity.PlanEntity;
import com.sme.be_sme.modules.billing.infrastructure.persistence.entity.SubscriptionEntity;
import com.sme.be_sme.modules.company.infrastructure.mapper.CompanyMapper;
import com.sme.be_sme.modules.company.infrastructure.persistence.entity.CompanyEntity;
import com.sme.be_sme.modules.identity.infrastructure.mapper.UserMapper;
import com.sme.be_sme.modules.identity.infrastructure.persistence.entity.UserEntity;
import com.sme.be_sme.modules.onboarding.infrastructure.mapper.OnboardingInstanceMapper;
import com.sme.be_sme.modules.onboarding.infrastructure.persistence.entity.OnboardingInstanceEntity;
import com.sme.be_sme.modules.platform.api.request.PlatformRiskDashboardRequest;
import com.sme.be_sme.modules.platform.api.response.PlatformRiskDashboardResponse;
import com.sme.be_sme.modules.platform.processor.analytics.PlatformAnalyticsSupport;
import com.sme.be_sme.shared.gateway.core.BaseBizProcessor;
import com.sme.be_sme.shared.gateway.core.BizContext;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PlatformRiskDashboardProcessor extends BaseBizProcessor<BizContext> {

    private final ObjectMapper objectMapper;
    private final OnboardingInstanceMapper onboardingInstanceMapper;
    private final PaymentTransactionMapper paymentTransactionMapper;
    private final CompanyMapper companyMapper;
    private final SubscriptionMapper subscriptionMapper;
    private final PlanMapper planMapper;
    private final UserMapper userMapper;

    @Override
    protected Object doProcess(BizContext context, JsonNode payload) {
        PlatformRiskDashboardRequest request = objectMapper.convertValue(payload, PlatformRiskDashboardRequest.class);
        java.util.Date startDate = PlatformAnalyticsSupport.parseDate(request.getStartDate(), true);
        java.util.Date endDate = PlatformAnalyticsSupport.parseDate(request.getEndDate(), false);

        int riskOnboardings = 0;
        Map<String, int[]> completionByCompany = new HashMap<>();
        for (OnboardingInstanceEntity item : onboardingInstanceMapper.selectAll()) {
            if (item == null || item.getCreatedAt() == null || !PlatformAnalyticsSupport.inRange(item.getCreatedAt(), startDate, endDate)) {
                continue;
            }
            String companyId = PlatformAnalyticsSupport.companyId(item);
            if (companyId != null) {
                completionByCompany.computeIfAbsent(companyId, k -> new int[2])[0]++;
                if ("COMPLETED".equalsIgnoreCase(item.getStatus())) {
                    completionByCompany.get(companyId)[1]++;
                }
            }
            if ("OVERDUE".equalsIgnoreCase(item.getStatus()) || "RISK".equalsIgnoreCase(item.getStatus())) {
                riskOnboardings++;
            }
        }

        int failedPayments = 0;
        for (PaymentTransactionEntity tx : paymentTransactionMapper.selectAll()) {
            if (tx != null && "FAILED".equalsIgnoreCase(tx.getStatus()) && PlatformAnalyticsSupport.inRange(tx.getCreatedAt(), startDate, endDate)) {
                failedPayments++;
            }
        }

        int suspendedCompanies = 0;
        Map<String, CompanyEntity> companyById = new HashMap<>();
        for (CompanyEntity company : companyMapper.selectAll()) {
            if (company != null && company.getCompanyId() != null) {
                companyById.put(company.getCompanyId(), company);
                if ("SUSPENDED".equalsIgnoreCase(company.getStatus())) {
                    suspendedCompanies++;
                }
            }
        }

        Map<String, PlanEntity> planById = new HashMap<>();
        for (PlanEntity plan : planMapper.selectAll()) {
            if (plan != null && plan.getPlanId() != null) {
                planById.put(plan.getPlanId(), plan);
            }
        }

        Map<String, Integer> employeeCountByCompany = new HashMap<>();
        for (UserEntity user : userMapper.selectAll()) {
            if (user != null && PlatformAnalyticsSupport.isEmployee(user) && user.getCompanyId() != null) {
                employeeCountByCompany.merge(user.getCompanyId(), 1, Integer::sum);
            }
        }

        int companiesNearPlanLimit = 0;
        int expiringSubscriptions = 0;
        long sevenDaysMs = 7L * 24 * 60 * 60 * 1000;
        long now = System.currentTimeMillis();
        for (SubscriptionEntity sub : subscriptionMapper.selectAll()) {
            if (sub == null || sub.getCompanyId() == null) {
                continue;
            }
            if (sub.getCurrentPeriodEnd() != null && sub.getCurrentPeriodEnd().getTime() - now <= sevenDaysMs && sub.getCurrentPeriodEnd().getTime() >= now) {
                expiringSubscriptions++;
            }
            PlanEntity plan = planById.get(sub.getPlanId());
            Integer employeeLimit = PlatformAnalyticsSupport.readInteger(plan, "getEmployeeLimit", "employeeLimit");
            int employeeCount = employeeCountByCompany.getOrDefault(sub.getCompanyId(), 0);
            if (employeeLimit != null && employeeLimit > 0 && employeeCount >= Math.ceil(employeeLimit * 0.8)) {
                companiesNearPlanLimit++;
            }
        }

        int lowCompletionCompanies = 0;
        List<PlatformRiskDashboardResponse.RiskCompanyItem> lowCompletionItems = new ArrayList<>();
        for (Map.Entry<String, int[]> entry : completionByCompany.entrySet()) {
            int total = entry.getValue()[0];
            int completed = entry.getValue()[1];
            double rate = total > 0 ? (double) completed / total : 0.0;
            if (rate < 0.5) {
                lowCompletionCompanies++;
                PlatformRiskDashboardResponse.RiskCompanyItem item = new PlatformRiskDashboardResponse.RiskCompanyItem();
                item.setCompanyId(entry.getKey());
                item.setCompanyName(companyById.containsKey(entry.getKey()) ? companyById.get(entry.getKey()).getName() : null);
                item.setCompletionRate(rate);
                lowCompletionItems.add(item);
            }
        }

        PlatformRiskDashboardResponse response = new PlatformRiskDashboardResponse();
        response.setRiskOnboardings(riskOnboardings);
        response.setFailedPayments(failedPayments);
        response.setSuspendedCompanies(suspendedCompanies);
        response.setCompaniesNearPlanLimit(companiesNearPlanLimit);
        response.setExpiringSubscriptions(expiringSubscriptions);
        response.setLowCompletionCompanies(lowCompletionCompanies);
        response.setLowCompletionCompanyItems(lowCompletionItems);
        return response;
    }
}
