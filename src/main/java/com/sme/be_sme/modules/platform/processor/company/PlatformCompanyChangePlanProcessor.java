package com.sme.be_sme.modules.platform.processor.company;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.billing.infrastructure.mapper.PlanMapper;
import com.sme.be_sme.modules.billing.infrastructure.mapper.SubscriptionMapper;
import com.sme.be_sme.modules.billing.infrastructure.persistence.entity.PlanEntity;
import com.sme.be_sme.modules.billing.infrastructure.persistence.entity.SubscriptionEntity;
import com.sme.be_sme.modules.platform.api.request.PlatformCompanyChangePlanRequest;
import com.sme.be_sme.modules.platform.api.response.PlatformCompanyChangePlanResponse;

import com.sme.be_sme.shared.constant.ErrorCodes;
import com.sme.be_sme.shared.exception.AppException;
import com.sme.be_sme.shared.gateway.core.BaseBizProcessor;
import com.sme.be_sme.shared.gateway.core.BizContext;
import lombok.RequiredArgsConstructor;
import org.flywaydb.core.internal.util.StringUtils;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
@RequiredArgsConstructor
public class PlatformCompanyChangePlanProcessor extends BaseBizProcessor<BizContext> {

    private final ObjectMapper objectMapper;
    private final SubscriptionMapper subscriptionMapper;
    private final PlanMapper planMapper;

    @Override
    protected Object doProcess(BizContext context, JsonNode payload) {
        PlatformCompanyChangePlanRequest request =
                objectMapper.convertValue(payload, PlatformCompanyChangePlanRequest.class);

        SubscriptionEntity subscription = subscriptionMapper.selectByPrimaryKey(request.getSubscriptionId());
        if (subscription == null) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "Subscription not found");
        }

        PlanEntity newPlan = planMapper.selectByPrimaryKey(request.getNewPlanId());
        if (newPlan == null) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "New plan not found");
        }

        String oldPlanId = subscription.getPlanId();

        subscription.setPlanId(request.getNewPlanId());
        if (StringUtils.hasText(request.getBillingCycle())) {
            subscription.setBillingCycle(request.getBillingCycle());
        }
        subscription.setUpdatedAt(new Date());
        subscriptionMapper.updateByPrimaryKey(subscription);

        PlatformCompanyChangePlanResponse response = new PlatformCompanyChangePlanResponse();
        response.setCompanyId(subscription.getCompanyId());
        response.setSubscriptionId(subscription.getSubscriptionId());
        response.setOldPlanId(oldPlanId);
        response.setNewPlanId(subscription.getPlanId());
        response.setBillingCycle(subscription.getBillingCycle());
        response.setMessage("Company plan changed successfully");
        return response;
    }
}