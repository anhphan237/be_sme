package com.sme.be_sme.modules.platform.processor.subscription;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.billing.infrastructure.mapper.PlanMapper;
import com.sme.be_sme.modules.billing.infrastructure.mapper.SubscriptionMapper;
import com.sme.be_sme.modules.billing.infrastructure.persistence.entity.PlanEntity;
import com.sme.be_sme.modules.billing.infrastructure.persistence.entity.SubscriptionEntity;
import com.sme.be_sme.modules.company.infrastructure.mapper.CompanyMapper;
import com.sme.be_sme.modules.company.infrastructure.persistence.entity.CompanyEntity;
import com.sme.be_sme.modules.platform.api.request.PlatformSubscriptionListRequest;
import com.sme.be_sme.modules.platform.api.response.PlatformSubscriptionListResponse;
import com.sme.be_sme.modules.platform.api.response.PlatformSubscriptionListResponse.SubscriptionItem;
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
public class PlatformSubscriptionListProcessor extends BaseBizProcessor<BizContext> {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_SIZE = 20;

    private final ObjectMapper objectMapper;
    private final SubscriptionMapper subscriptionMapper;
    private final CompanyMapper companyMapper;
    private final PlanMapper planMapper;

    @Override
    protected Object doProcess(BizContext context, JsonNode payload) {
        PlatformSubscriptionListRequest request = objectMapper.convertValue(payload, PlatformSubscriptionListRequest.class);

        int page = request.getPage() != null && request.getPage() > 0 ? request.getPage() : DEFAULT_PAGE;
        int size = request.getSize() != null && request.getSize() > 0 ? request.getSize() : DEFAULT_SIZE;

        List<SubscriptionEntity> allSubscriptions = subscriptionMapper.selectAll();
        Map<String, String> companyNameMap = buildCompanyNameMap();
        Map<String, PlanEntity> planMap = buildPlanMap();

        Map<String, String> planCodeByPlanId = new HashMap<>();
        for (PlanEntity plan : planMap.values()) {
            if (plan != null && plan.getPlanId() != null) {
                planCodeByPlanId.put(plan.getPlanId(), plan.getCode());
            }
        }

        List<SubscriptionEntity> filtered = new ArrayList<>();
        for (SubscriptionEntity sub : allSubscriptions) {
            if (sub == null) {
                continue;
            }

            if (StringUtils.hasText(request.getStatus())) {
                String requestStatus = normalize(request.getStatus());
                String subscriptionStatus = normalize(sub.getStatus());
                if (subscriptionStatus == null || !requestStatus.equals(subscriptionStatus)) {
                    continue;
                }
            }

            if (StringUtils.hasText(request.getPlanCode())) {
                String code = sub.getPlanId() != null ? planCodeByPlanId.get(sub.getPlanId()) : null;
                if (code == null || !normalize(request.getPlanCode()).equals(normalize(code))) {
                    continue;
                }
            }

            filtered.add(sub);
        }

        int total = filtered.size();
        int fromIndex = Math.min((page - 1) * size, total);
        int toIndex = Math.min(fromIndex + size, total);
        List<SubscriptionEntity> pageSlice = filtered.subList(fromIndex, toIndex);

        List<SubscriptionItem> items = new ArrayList<>();
        for (SubscriptionEntity sub : pageSlice) {
            SubscriptionItem item = new SubscriptionItem();
            item.setSubscriptionId(sub.getSubscriptionId());
            item.setCompanyId(sub.getCompanyId());
            item.setCompanyName(companyNameMap.get(sub.getCompanyId()));
            item.setPlanCode(sub.getPlanId() != null ? planCodeByPlanId.get(sub.getPlanId()) : null);
            item.setStatus(sub.getStatus());
            item.setBillingCycle(sub.getBillingCycle());
            item.setCurrentPeriodStart(sub.getCurrentPeriodStart());
            item.setCurrentPeriodEnd(sub.getCurrentPeriodEnd());
            items.add(item);
        }

        PlatformSubscriptionListResponse response = new PlatformSubscriptionListResponse();
        response.setItems(items);
        response.setTotal(total);
        return response;
    }

    private String normalize(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim()
                .replace("-", "_")
                .replace(" ", "_")
                .toUpperCase();
    }

    private Map<String, String> buildCompanyNameMap() {
        Map<String, String> map = new HashMap<>();
        for (CompanyEntity company : companyMapper.selectAll()) {
            if (company != null && company.getCompanyId() != null) {
                map.put(company.getCompanyId(), company.getName());
            }
        }
        return map;
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