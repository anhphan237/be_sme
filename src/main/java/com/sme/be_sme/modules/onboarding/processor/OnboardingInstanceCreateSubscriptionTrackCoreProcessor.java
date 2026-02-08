package com.sme.be_sme.modules.onboarding.processor;

import com.sme.be_sme.modules.billing.api.request.UsageTrackRequest;
import com.sme.be_sme.modules.billing.facade.BillingFacade;
import com.sme.be_sme.modules.billing.infrastructure.mapper.SubscriptionMapper;
import com.sme.be_sme.modules.billing.infrastructure.persistence.entity.SubscriptionEntity;
import com.sme.be_sme.modules.onboarding.context.OnboardingInstanceCreateContext;
import com.sme.be_sme.shared.gateway.core.BaseCoreProcessor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class OnboardingInstanceCreateSubscriptionTrackCoreProcessor
        extends BaseCoreProcessor<OnboardingInstanceCreateContext> {

    private final BillingFacade billingFacade;
    private final SubscriptionMapper subscriptionMapper;

    @Override
    protected Object process(OnboardingInstanceCreateContext ctx) {
        String companyId = ctx.getBiz().getTenantId();
        List<SubscriptionEntity> all = subscriptionMapper.selectAll();
        SubscriptionEntity subscription = all == null ? null : all.stream()
                .filter(s -> s != null && companyId.equals(s.getCompanyId()))
                .findFirst()
                .orElse(null);
        if (subscription == null) {
            return null;
        }
        UsageTrackRequest req = new UsageTrackRequest();
        req.setSubscriptionId(subscription.getSubscriptionId());
        req.setUsageType("ONBOARDED_EMPLOYEE");
        req.setQuantity(1);
        billingFacade.trackUsage(req);
        return null;
    }
}
