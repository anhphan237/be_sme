package com.sme.be_sme.modules.platform.processor.plan;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.billing.infrastructure.mapper.PlanMapper;
import com.sme.be_sme.modules.billing.infrastructure.persistence.entity.PlanEntity;
import com.sme.be_sme.modules.platform.api.request.PlatformPlanListRequest;
import com.sme.be_sme.modules.platform.api.response.PlatformPlanListResponse;
import com.sme.be_sme.modules.platform.api.response.PlatformPlanListResponse.PlanItem;
import com.sme.be_sme.shared.gateway.core.BaseBizProcessor;
import com.sme.be_sme.shared.gateway.core.BizContext;
import com.sme.be_sme.shared.util.StorageUnitConverter;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class PlatformPlanListProcessor extends BaseBizProcessor<BizContext> {

    private final ObjectMapper objectMapper;
    private final PlanMapper planMapper;

    @Override
    protected Object doProcess(BizContext context, JsonNode payload) {
        PlatformPlanListRequest request = objectMapper.convertValue(payload, PlatformPlanListRequest.class);

        List<PlanEntity> allPlans = planMapper.selectAll();
        List<PlanItem> items = new ArrayList<>();

        for (PlanEntity plan : allPlans) {
            if (plan == null) continue;
            if (plan.getCompanyId() != null) continue;

            if (StringUtils.hasText(request.getStatus())
                    && !request.getStatus().equalsIgnoreCase(plan.getStatus())) {
                continue;
            }

            PlanItem item = new PlanItem();
            item.setPlanId(plan.getPlanId());
            item.setCode(plan.getCode());
            item.setName(plan.getName());
            item.setEmployeeLimitPerMonth(plan.getEmployeeLimitPerMonth());
            item.setOnboardingTemplateLimit(plan.getOnboardingTemplateLimit());
            item.setEventTemplateLimit(plan.getEventTemplateLimit());
            item.setDocumentLimit(plan.getDocumentLimit());
            item.setStorageLimitBytes(plan.getStorageLimitBytes());
            item.setStorageLimitMb(StorageUnitConverter.toMb(plan.getStorageLimitBytes()));
            item.setStorageLimitGb(StorageUnitConverter.toGb(plan.getStorageLimitBytes()));
            item.setPriceVndMonthly(plan.getPriceVndMonthly());
            item.setPriceVndYearly(plan.getPriceVndYearly());
            item.setStatus(plan.getStatus());
            items.add(item);
        }

        PlatformPlanListResponse response = new PlatformPlanListResponse();
        response.setItems(items);
        return response;
    }
}
