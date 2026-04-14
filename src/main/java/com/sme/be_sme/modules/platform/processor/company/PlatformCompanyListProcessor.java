package com.sme.be_sme.modules.platform.processor.company;

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
import com.sme.be_sme.modules.platform.api.request.PlatformCompanyListRequest;
import com.sme.be_sme.modules.platform.api.response.PlatformCompanyListResponse;
import com.sme.be_sme.modules.platform.api.response.PlatformCompanyListResponse.CompanyItem;
import com.sme.be_sme.shared.gateway.core.BaseBizProcessor;
import com.sme.be_sme.shared.gateway.core.BizContext;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class PlatformCompanyListProcessor extends BaseBizProcessor<BizContext> {

    private static final String PLATFORM_STATUS = "PLATFORM";
    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_SIZE = 20;

    private final ObjectMapper objectMapper;
    private final CompanyMapper companyMapper;
    private final UserMapper userMapper;
    private final SubscriptionMapper subscriptionMapper;
    private final PlanMapper planMapper;

    @Override
    protected Object doProcess(BizContext context, JsonNode payload) {
        PlatformCompanyListRequest request = objectMapper.convertValue(payload, PlatformCompanyListRequest.class);

        int page = request.getPage() != null && request.getPage() > 0 ? request.getPage() : DEFAULT_PAGE;
        int size = request.getSize() != null && request.getSize() > 0 ? request.getSize() : DEFAULT_SIZE;

        List<CompanyEntity> allCompanies = companyMapper.selectAll();
        List<UserEntity> allUsers = userMapper.selectAll();
        List<SubscriptionEntity> allSubscriptions = subscriptionMapper.selectAll();
        Map<String, PlanEntity> plansById = buildPlanMap();

        Map<String, Integer> userCountByCompany = new HashMap<>();
        for (UserEntity user : allUsers) {
            if (user != null && user.getCompanyId() != null) {
                userCountByCompany.merge(user.getCompanyId(), 1, Integer::sum);
            }
        }

        Map<String, SubscriptionEntity> subscriptionByCompany = buildCurrentSubscriptionByCompany(allSubscriptions);

        List<CompanyEntity> filtered = new ArrayList<>();
        for (CompanyEntity company : allCompanies) {
            if (company == null) {
                continue;
            }
            if (PLATFORM_STATUS.equalsIgnoreCase(company.getStatus())) {
                continue;
            }

            if (StringUtils.hasText(request.getStatus())
                    && !request.getStatus().trim().equalsIgnoreCase(String.valueOf(company.getStatus()).trim())) {
                continue;
            }

            if (StringUtils.hasText(request.getSearch())
                    && (company.getName() == null
                    || !company.getName().toLowerCase().contains(request.getSearch().trim().toLowerCase()))) {
                continue;
            }

            if (StringUtils.hasText(request.getPlanCode())) {
                SubscriptionEntity sub = subscriptionByCompany.get(company.getCompanyId());
                if (sub == null) {
                    continue;
                }

                PlanEntity plan = sub.getPlanId() != null ? plansById.get(sub.getPlanId()) : null;
                if (plan == null || !request.getPlanCode().trim().equalsIgnoreCase(String.valueOf(plan.getCode()).trim())) {
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
            CompanyItem item = new CompanyItem();
            item.setCompanyId(company.getCompanyId());
            item.setName(company.getName());
            item.setStatus(company.getStatus());
            item.setCreatedAt(company.getCreatedAt());
            item.setUserCount(userCountByCompany.getOrDefault(company.getCompanyId(), 0));

            SubscriptionEntity sub = subscriptionByCompany.get(company.getCompanyId());
            if (sub != null) {
                item.setSubscriptionStatus(sub.getStatus());
                PlanEntity plan = sub.getPlanId() != null ? plansById.get(sub.getPlanId()) : null;
                if (plan != null) {
                    item.setPlanCode(plan.getCode());
                }
            }
            items.add(item);
        }

        PlatformCompanyListResponse response = new PlatformCompanyListResponse();
        response.setItems(items);
        response.setTotal(total);
        return response;
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
    private Map<String, SubscriptionEntity> buildCurrentSubscriptionByCompany(List<SubscriptionEntity> subscriptions) {
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

        java.util.Date candidateDate = candidate.getUpdatedAt() != null ? candidate.getUpdatedAt() : candidate.getCreatedAt();
        java.util.Date currentDate = current.getUpdatedAt() != null ? current.getUpdatedAt() : current.getCreatedAt();

        if (candidateDate == null) {
            return false;
        }
        if (currentDate == null) {
            return true;
        }

        return candidateDate.after(currentDate);
    }
}