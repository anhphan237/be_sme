package com.sme.be_sme.modules.platform.processor.subscription;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.billing.infrastructure.mapper.PlanMapper;
import com.sme.be_sme.modules.billing.infrastructure.mapper.SubscriptionMapper;
import com.sme.be_sme.modules.billing.infrastructure.persistence.entity.PlanEntity;
import com.sme.be_sme.modules.billing.infrastructure.persistence.entity.SubscriptionEntity;
import com.sme.be_sme.modules.company.infrastructure.mapper.CompanyMapper;
import com.sme.be_sme.modules.company.infrastructure.persistence.entity.CompanyEntity;
import com.sme.be_sme.modules.platform.api.request.PlatformSubscriptionDetailRequest;
import com.sme.be_sme.modules.platform.api.response.PlatformSubscriptionDetailResponse;
import com.sme.be_sme.shared.constant.ErrorCodes;
import com.sme.be_sme.shared.exception.AppException;
import com.sme.be_sme.shared.gateway.core.BaseBizProcessor;
import com.sme.be_sme.shared.gateway.core.BizContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class PlatformSubscriptionDetailProcessor extends BaseBizProcessor<BizContext> {

    private final ObjectMapper objectMapper;
    private final SubscriptionMapper subscriptionMapper;
    private final CompanyMapper companyMapper;
    private final PlanMapper planMapper;

    @Override
    protected Object doProcess(BizContext context, JsonNode payload) {
        PlatformSubscriptionDetailRequest request = objectMapper.convertValue(payload, PlatformSubscriptionDetailRequest.class);

        if (!StringUtils.hasText(request.getSubscriptionId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "subscriptionId is required");
        }

        SubscriptionEntity subscription = subscriptionMapper.selectByPrimaryKey(request.getSubscriptionId());
        if (subscription == null) {
            throw AppException.of(ErrorCodes.NOT_FOUND, "Subscription not found");
        }

        String companyName = null;
        if (subscription.getCompanyId() != null) {
            CompanyEntity company = companyMapper.selectByPrimaryKey(subscription.getCompanyId());
            if (company != null) {
                companyName = company.getName();
            }
        }

        String planCode = null;
        String planName = null;
        if (subscription.getPlanId() != null) {
            PlanEntity plan = planMapper.selectByPrimaryKey(subscription.getPlanId());
            if (plan != null) {
                planCode = plan.getCode();
                planName = plan.getName();
            }
        }

        PlatformSubscriptionDetailResponse response = new PlatformSubscriptionDetailResponse();
        response.setSubscriptionId(subscription.getSubscriptionId());
        response.setCompanyId(subscription.getCompanyId());
        response.setCompanyName(companyName);
        response.setPlanCode(planCode);
        response.setPlanName(planName);
        response.setStatus(subscription.getStatus());
        response.setBillingCycle(subscription.getBillingCycle());
        response.setCurrentPeriodStart(subscription.getCurrentPeriodStart());
        response.setCurrentPeriodEnd(subscription.getCurrentPeriodEnd());
        response.setAutoRenew(subscription.getAutoRenew());
        return response;
    }
}
