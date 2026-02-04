package com.sme.be_sme.modules.onboarding.context;

import com.sme.be_sme.modules.onboarding.api.request.OnboardingTemplateListRequest;
import com.sme.be_sme.modules.onboarding.api.response.OnboardingTemplateListResponse;
import com.sme.be_sme.modules.onboarding.infrastructure.persistence.entity.OnboardingTemplateEntity;
import com.sme.be_sme.shared.gateway.core.BizContext;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class OnboardingTemplateListContext {
    private BizContext biz;
    private OnboardingTemplateListRequest request;
    private OnboardingTemplateListResponse response;

    private List<OnboardingTemplateEntity> templates;
}