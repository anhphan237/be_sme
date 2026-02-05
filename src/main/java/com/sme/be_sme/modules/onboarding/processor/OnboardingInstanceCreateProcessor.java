package com.sme.be_sme.modules.onboarding.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.onboarding.api.request.OnboardingInstanceCreateRequest;
import com.sme.be_sme.modules.onboarding.api.response.OnboardingInstanceResponse;
import com.sme.be_sme.modules.onboarding.context.OnboardingInstanceCreateContext;
import com.sme.be_sme.shared.gateway.core.BaseCoreProcessor;
import com.sme.be_sme.shared.gateway.core.BizContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class OnboardingInstanceCreateProcessor
        extends BaseCoreProcessor<OnboardingInstanceCreateContext> {

    private final ObjectMapper objectMapper;

    private final OnboardingInstanceCreateValidateCoreProcessor validate;
    private final OnboardingInstanceCreateIdempotencyCoreProcessor idempotency;
    private final OnboardingInstanceCreateLoadTemplateCoreProcessor loadTemplate;
    private final OnboardingInstanceCreateSubscriptionTrackCoreProcessor subscriptionTrack;
    private final OnboardingInstanceCreateCloneCoreProcessor clone;
    private final OnboardingInstanceCreateBuildResponseCoreProcessor buildRes;

    @Override
    protected OnboardingInstanceCreateContext buildContext(BizContext biz, JsonNode payload) {
        OnboardingInstanceCreateRequest req =
                objectMapper.convertValue(payload, OnboardingInstanceCreateRequest.class);

        OnboardingInstanceCreateContext ctx = new OnboardingInstanceCreateContext();
        ctx.setBiz(biz);
        ctx.setRequest(req);
        ctx.setResponse(new OnboardingInstanceResponse());
        return ctx;
    }

    @Override
    @Transactional
    protected Object process(OnboardingInstanceCreateContext ctx) {
        validate.processWith(ctx);
        idempotency.processWith(ctx);
        if (ctx.getExistingInstance() != null) {
            buildRes.processWith(ctx);
            return ctx.getResponse();
        }
        loadTemplate.processWith(ctx);
        subscriptionTrack.processWith(ctx);
        clone.processWith(ctx);
        buildRes.processWith(ctx);
        return ctx.getResponse();
    }
}

