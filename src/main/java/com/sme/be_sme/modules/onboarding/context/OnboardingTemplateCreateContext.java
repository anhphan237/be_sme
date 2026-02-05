package com.sme.be_sme.modules.onboarding.context;

import com.sme.be_sme.modules.onboarding.api.request.OnboardingTemplateCreateRequest;
import com.sme.be_sme.modules.onboarding.api.response.OnboardingTemplateResponse;
import com.sme.be_sme.modules.onboarding.infrastructure.persistence.entity.OnboardingTemplateEntity;
import com.sme.be_sme.shared.gateway.core.BizContext;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
public class OnboardingTemplateCreateContext {
    private BizContext biz;
    private OnboardingTemplateCreateRequest request;
    private String companyId;
    private String templateId;
    private Date now;
    private OnboardingTemplateEntity templateEntity;
    private OnboardingTemplateResponse response;
}
