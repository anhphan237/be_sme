package com.sme.be_sme.modules.platform.processor.analytics;

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
import com.sme.be_sme.modules.platform.api.request.PlatformEmployeeAnalyticsRequest;
import com.sme.be_sme.modules.platform.api.response.PlatformEmployeeAnalyticsResponse;
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
public class PlatformEmployeeAnalyticsProcessor extends BaseBizProcessor<BizContext> {

    private final ObjectMapper objectMapper;
    private final UserMapper userMapper;
    private final CompanyMapper companyMapper;
    private final SubscriptionMapper subscriptionMapper;
    private final PlanMapper planMapper;

    @Override
    protected Object doProcess(BizContext context, JsonNode payload) {
        PlatformEmployeeAnalyticsRequest request = objectMapper.convertValue(payload, PlatformEmployeeAnalyticsRequest.class);
        java.util.Date startDate = PlatformAnalyticsSupport.parseDate(request.getStartDate(), true);
        java.util.Date endDate = PlatformAnalyticsSupport.parseDate(request.getEndDate(), false);

        List<UserEntity> users = userMapper.selectAll();
        Map<String, CompanyEntity> companiesById = new HashMap<>();
        for (CompanyEntity company : companyMapper.selectAll()) {
            if (company != null && company.getCompanyId() != null) {
                companiesById.put(company.getCompanyId(), company);
            }
        }
        Map<String, SubscriptionEntity> subscriptionByCompany = new HashMap<>();
        for (SubscriptionEntity sub : subscriptionMapper.selectAll()) {
            if (sub != null && sub.getCompanyId() != null) {
                subscriptionByCompany.put(sub.getCompanyId(), sub);
            }
        }
        Map<String, PlanEntity> planById = new HashMap<>();
        for (PlanEntity plan : planMapper.selectAll()) {
            if (plan != null && plan.getPlanId() != null) {
                planById.put(plan.getPlanId(), plan);
            }
        }

        int totalEmployees = 0;
        int activeEmployees = 0;
        int newEmployeesInRange = 0;
        Map<String, Integer> employeesByCompany = new HashMap<>();
        Map<String, Integer> employeesByPlan = new HashMap<>();

        for (UserEntity user : users) {
            if (user == null || !PlatformAnalyticsSupport.isEmployee(user)) {
                continue;
            }
            totalEmployees++;
            activeEmployees++;
            if (PlatformAnalyticsSupport.inRange(user.getCreatedAt(), startDate, endDate)) {
                newEmployeesInRange++;
            }
            String companyId = user.getCompanyId();
            if (companyId != null) {
                employeesByCompany.merge(companyId, 1, Integer::sum);
                SubscriptionEntity sub = subscriptionByCompany.get(companyId);
                if (sub != null && sub.getPlanId() != null) {
                    employeesByPlan.merge(sub.getPlanId(), 1, Integer::sum);
                }
            }
        }

        List<PlatformEmployeeAnalyticsResponse.EmployeeByCompanyItem> byCompanyItems = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : employeesByCompany.entrySet()) {
            PlatformEmployeeAnalyticsResponse.EmployeeByCompanyItem item = new PlatformEmployeeAnalyticsResponse.EmployeeByCompanyItem();
            item.setCompanyId(entry.getKey());
            item.setCompanyName(companiesById.containsKey(entry.getKey()) ? companiesById.get(entry.getKey()).getName() : null);
            item.setEmployeeCount(entry.getValue());
            byCompanyItems.add(item);
        }

        List<PlatformEmployeeAnalyticsResponse.EmployeeByPlanItem> byPlanItems = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : employeesByPlan.entrySet()) {
            PlanEntity plan = planById.get(entry.getKey());
            PlatformEmployeeAnalyticsResponse.EmployeeByPlanItem item = new PlatformEmployeeAnalyticsResponse.EmployeeByPlanItem();
            item.setPlanId(entry.getKey());
            item.setPlanCode(plan != null ? PlatformAnalyticsSupport.planCode(plan) : null);
            item.setPlanName(plan != null ? PlatformAnalyticsSupport.planName(plan) : null);
            item.setEmployeeCount(entry.getValue());
            byPlanItems.add(item);
        }

        PlatformEmployeeAnalyticsResponse response = new PlatformEmployeeAnalyticsResponse();
        response.setTotalEmployees(totalEmployees);
        response.setActiveEmployees(activeEmployees);
        response.setNewEmployeesInRange(newEmployeesInRange);
        response.setEmployeesByCompany(byCompanyItems);
        response.setEmployeesByPlan(byPlanItems);
        return response;
    }
}
