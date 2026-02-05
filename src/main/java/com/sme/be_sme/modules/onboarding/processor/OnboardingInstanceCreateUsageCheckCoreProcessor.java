package com.sme.be_sme.modules.onboarding.processor;

import com.sme.be_sme.modules.billing.api.request.PlanGetRequest;
import com.sme.be_sme.modules.billing.api.request.UsageCheckRequest;
import com.sme.be_sme.modules.billing.api.response.PlanGetResponse;
import com.sme.be_sme.modules.billing.api.response.UsageCheckResponse;
import com.sme.be_sme.modules.billing.facade.BillingFacade;
import com.sme.be_sme.modules.onboarding.context.OnboardingInstanceCreateContext;
import com.sme.be_sme.shared.constant.ErrorCodes;
import com.sme.be_sme.shared.exception.AppException;
import com.sme.be_sme.shared.gateway.core.BaseCoreProcessor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OnboardingInstanceCreateUsageCheckCoreProcessor
        extends BaseCoreProcessor<OnboardingInstanceCreateContext> {

    private final BillingFacade billingFacade;

    @Override
    protected Object process(OnboardingInstanceCreateContext ctx) {
        if (ctx.getExistingInstance() != null) {
            return null;
        }

        UsageCheckResponse usage = billingFacade.checkUsage(new UsageCheckRequest());
        PlanGetResponse plan;
        try {
            plan = billingFacade.getPlan(new PlanGetRequest());
        } catch (Exception e) {
            return null;
        }
        if (plan == null || plan.getEmployeeLimitPerMonth() == null || plan.getEmployeeLimitPerMonth() <= 0) {
            return null;
        }

        int currentUsage = usage.getCurrentUsage();
        int planLimit = plan.getEmployeeLimitPerMonth();
        if (currentUsage >= planLimit) {
            throw AppException.of(ErrorCodes.LIMIT_EXCEEDED, "Your current plan limit has been reached");
        }
        return null;
    }
}
