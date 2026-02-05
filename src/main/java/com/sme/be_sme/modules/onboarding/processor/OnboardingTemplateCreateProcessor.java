package com.sme.be_sme.modules.onboarding.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.onboarding.api.request.OnboardingTemplateCreateRequest;
import com.sme.be_sme.modules.onboarding.context.OnboardingTemplateCreateContext;
import com.sme.be_sme.modules.onboarding.processor.template.core.OnboardingTemplateCreateBuildResponseCoreProcessor;
import com.sme.be_sme.modules.onboarding.processor.template.core.OnboardingTemplateCreateChecklistsAndTasksCoreProcessor;
import com.sme.be_sme.modules.onboarding.processor.template.core.OnboardingTemplateCreateInsertTemplateCoreProcessor;
import com.sme.be_sme.modules.onboarding.processor.template.core.OnboardingTemplateCreateValidateCoreProcessor;
import com.sme.be_sme.shared.gateway.core.BaseCoreProcessor;
import com.sme.be_sme.shared.gateway.core.BizContext;
import com.sme.be_sme.shared.util.UuidGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

@Component
@RequiredArgsConstructor
public class OnboardingTemplateCreateProcessor extends BaseCoreProcessor<OnboardingTemplateCreateContext> {

    private final ObjectMapper objectMapper;
    private final OnboardingTemplateCreateValidateCoreProcessor validate;
    private final OnboardingTemplateCreateInsertTemplateCoreProcessor insertTemplate;
    private final OnboardingTemplateCreateChecklistsAndTasksCoreProcessor createChecklistsAndTasks;
    private final OnboardingTemplateCreateBuildResponseCoreProcessor buildResponse;

    @Override
    protected OnboardingTemplateCreateContext buildContext(BizContext biz, JsonNode payload) {
        OnboardingTemplateCreateRequest request = objectMapper.convertValue(payload, OnboardingTemplateCreateRequest.class);
        OnboardingTemplateCreateContext ctx = new OnboardingTemplateCreateContext();
        ctx.setBiz(biz);
        ctx.setRequest(request);
        ctx.setCompanyId(biz.getTenantId());
        ctx.setTemplateId(UuidGenerator.generate());
        ctx.setNow(new Date());
        return ctx;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    protected Object process(OnboardingTemplateCreateContext ctx) {
        validate.processWith(ctx);
        insertTemplate.processWith(ctx);
        createChecklistsAndTasks.processWith(ctx);
        buildResponse.processWith(ctx);
        return ctx.getResponse();
    }
}
