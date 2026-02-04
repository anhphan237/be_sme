package com.sme.be_sme.modules.onboarding.processor.template;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.onboarding.api.request.OnboardingTemplateGetRequest;
import com.sme.be_sme.modules.onboarding.api.response.OnboardingTemplateGetResponse;
import com.sme.be_sme.modules.onboarding.context.OnboardingTemplateGetContext;
import com.sme.be_sme.modules.onboarding.processor.template.core.OnboardingTemplateGetBuildResponseCoreProcessor;
import com.sme.be_sme.modules.onboarding.processor.template.core.OnboardingTemplateGetLoadCoreProcessor;
import com.sme.be_sme.modules.onboarding.processor.template.core.OnboardingTemplateGetValidateCoreProcessor;
import com.sme.be_sme.shared.gateway.core.BaseCoreProcessor;
import com.sme.be_sme.shared.gateway.core.BizContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class OnboardingTemplateGetProcessor extends BaseCoreProcessor<OnboardingTemplateGetContext> {

    private final ObjectMapper objectMapper;

    private final OnboardingTemplateGetValidateCoreProcessor validate;
    private final OnboardingTemplateGetLoadCoreProcessor load;
    private final OnboardingTemplateGetBuildResponseCoreProcessor buildRes;

    @Override
    protected OnboardingTemplateGetContext buildContext(BizContext biz, JsonNode payload) {
        OnboardingTemplateGetRequest req = objectMapper.convertValue(payload, OnboardingTemplateGetRequest.class);

        OnboardingTemplateGetContext ctx = new OnboardingTemplateGetContext();
        ctx.setBiz(biz);
        ctx.setRequest(req);
        ctx.setResponse(new OnboardingTemplateGetResponse());
        return ctx;
    }

    @Override
    @Transactional(readOnly = true)
    protected Object process(OnboardingTemplateGetContext ctx) {
        validate.processWith(ctx);
        load.processWith(ctx);
        buildRes.processWith(ctx);
        return ctx.getResponse();
    }
}
