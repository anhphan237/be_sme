package com.sme.be_sme.modules.platform.context;

import com.sme.be_sme.modules.onboarding.infrastructure.persistence.entity.OnboardingTemplateEntity;
import com.sme.be_sme.modules.platform.api.request.CreatePlatformTemplateRequest;
import com.sme.be_sme.modules.platform.api.response.CreatePlatformTemplateResponse;
import com.sme.be_sme.shared.gateway.core.BizContext;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
public class PlatformCreateTemplateContext {
    private BizContext biz;
    private CreatePlatformTemplateRequest request;
    private String companyId;
    private String templateId;
    private Date now;
    private OnboardingTemplateEntity templateEntity;
    private CreatePlatformTemplateResponse response;
}
