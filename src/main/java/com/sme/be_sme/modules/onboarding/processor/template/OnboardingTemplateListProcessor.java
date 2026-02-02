package com.sme.be_sme.modules.onboarding.processor.template;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.onboarding.api.request.OnboardingTemplateListRequest;
import com.sme.be_sme.modules.onboarding.api.response.OnboardingTemplateListResponse;
import com.sme.be_sme.modules.onboarding.context.OnboardingTemplateListContext;
import com.sme.be_sme.modules.onboarding.processor.template.core.OnboardingTemplateListCoreProcessor;
import com.sme.be_sme.modules.onboarding.processor.template.core.OnboardingTemplateListValidateCoreProcessor;
import com.sme.be_sme.shared.gateway.core.BaseCoreProcessor;
import com.sme.be_sme.shared.gateway.core.BizContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class OnboardingTemplateListProcessor extends BaseCoreProcessor<OnboardingTemplateListContext> {

    private final ObjectMapper objectMapper;

    private final OnboardingTemplateListValidateCoreProcessor validate;
    private final OnboardingTemplateListCoreProcessor listCore;

    @Override
    protected OnboardingTemplateListContext buildContext(BizContext biz, JsonNode payload) {
        OnboardingTemplateListRequest req = objectMapper.convertValue(payload, OnboardingTemplateListRequest.class);

        OnboardingTemplateListContext ctx = new OnboardingTemplateListContext();
        ctx.setBiz(biz);
        ctx.setRequest(req);
        ctx.setResponse(new OnboardingTemplateListResponse());
        return ctx;
    }

    @Override
    @Transactional(readOnly = true)
    protected Object process(OnboardingTemplateListContext ctx) {
        validate.processWith(ctx);
        listCore.processWith(ctx);

        // response đã được build trong core (hoặc bạn có thể build tại đây)
        return ctx.getResponse();
    }
}

