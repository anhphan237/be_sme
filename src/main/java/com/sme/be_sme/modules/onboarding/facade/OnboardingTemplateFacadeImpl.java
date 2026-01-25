package com.sme.be_sme.modules.onboarding.facade;

import com.sme.be_sme.modules.onboarding.api.request.OnboardingTemplateCreateRequest;
import com.sme.be_sme.modules.onboarding.api.request.OnboardingTemplateUpdateRequest;
import com.sme.be_sme.modules.onboarding.api.response.OnboardingTemplateResponse;
import com.sme.be_sme.modules.onboarding.processor.OnboardingTemplateCreateProcessor;
import com.sme.be_sme.modules.onboarding.processor.OnboardingTemplateUpdateProcessor;
import com.sme.be_sme.shared.gateway.core.BaseOperationFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OnboardingTemplateFacadeImpl extends BaseOperationFacade implements OnboardingTemplateFacade {

    private final OnboardingTemplateCreateProcessor onboardingTemplateCreateProcessor;
    private final OnboardingTemplateUpdateProcessor onboardingTemplateUpdateProcessor;

    @Override
    public OnboardingTemplateResponse createOnboardingTemplate(OnboardingTemplateCreateRequest request) {
        return call(onboardingTemplateCreateProcessor, request, OnboardingTemplateResponse.class);
    }

    @Override
    public OnboardingTemplateResponse updateOnboardingTemplate(OnboardingTemplateUpdateRequest request) {
        return call(onboardingTemplateUpdateProcessor, request, OnboardingTemplateResponse.class);
    }
}
