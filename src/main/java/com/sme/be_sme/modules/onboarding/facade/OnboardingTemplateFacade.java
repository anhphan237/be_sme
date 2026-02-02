package com.sme.be_sme.modules.onboarding.facade;

import com.sme.be_sme.modules.onboarding.api.request.OnboardingTemplateCreateRequest;
import com.sme.be_sme.modules.onboarding.api.request.OnboardingTemplateGetRequest;
import com.sme.be_sme.modules.onboarding.api.request.OnboardingTemplateListRequest;
import com.sme.be_sme.modules.onboarding.api.request.OnboardingTemplateUpdateRequest;
import com.sme.be_sme.modules.onboarding.api.response.OnboardingTemplateGetResponse;
import com.sme.be_sme.modules.onboarding.api.response.OnboardingTemplateListResponse;
import com.sme.be_sme.modules.onboarding.api.response.OnboardingTemplateResponse;
import com.sme.be_sme.shared.gateway.annotation.OperationType;
import com.sme.be_sme.shared.gateway.core.OperationFacadeProvider;

public interface OnboardingTemplateFacade extends OperationFacadeProvider {

    @OperationType("com.sme.onboarding.template.create")
    OnboardingTemplateResponse createOnboardingTemplate(OnboardingTemplateCreateRequest request);

    @OperationType("com.sme.onboarding.template.update")
    OnboardingTemplateResponse updateOnboardingTemplate(OnboardingTemplateUpdateRequest request);

    @OperationType("com.sme.onboarding.template.list")
    OnboardingTemplateListResponse listOnboardingTemplates(OnboardingTemplateListRequest request);

    @OperationType("com.sme.onboarding.template.get")
    OnboardingTemplateGetResponse getOnboardingTemplate(OnboardingTemplateGetRequest request);
}
