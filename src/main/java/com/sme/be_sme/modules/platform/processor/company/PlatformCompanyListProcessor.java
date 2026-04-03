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
    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;

    private final ObjectMapper objectMapper;
    private final CompanyMapper companyMapper;
    private final UserMapper userMapper;
    private final SubscriptionMapper subscriptionMapper;
    private final PlanMapper planMapper;

    @Override
    protected Object doProcess(BizContext context, JsonNode payload) {
        PlatformCompanyListRequest request = objectMapper.convertValue(payload, PlatformCompanyListRequest.class);

        int page = request.getPage() != null ? request.getPage() : DEFAULT_PAGE;
        int size = request.getSize() != null ? request.getSize() : DEFAULT_SIZE;

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

        Map<String, SubscriptionEntity> subscriptionByCompany = new HashMap<>();
        for (SubscriptionEntity sub : allSubscriptions) {
            if (sub != null && sub.getCompanyId() != null) {
                subscriptionByCompany.put(sub.getCompanyId(), sub);
            }
        }

        List<CompanyEntity> filtered = new ArrayList<>();
        for (CompanyEntity company : allCompanies) {
            if (company == null) continue;
            if (PLATFORM_STATUS.equalsIgnoreCase(company.getStatus())) continue;

            if (StringUtils.hasText(request.getStatus())
                    && !request.getStatus().equalsIgnoreCase(company.getStatus())) {
                continue;
            }
            if (StringUtils.hasText(request.getSearch())
                    && (company.getName() == null
                        || !company.getName().toLowerCase().contains(request.getSearch().toLowerCase()))) {
                continue;
            }
            filtered.add(company);
        }

        int total = filtered.size();
        int fromIndex = Math.min(page * size, total);
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
}
