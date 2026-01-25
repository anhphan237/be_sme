package com.sme.be_sme.modules.onboarding.facade.impl;

import com.sme.be_sme.modules.onboarding.api.request.CompanySetupRequest;
import com.sme.be_sme.modules.onboarding.api.response.CompanySetupResponse;
import com.sme.be_sme.modules.onboarding.facade.OnboardingFacade;
import com.sme.be_sme.modules.onboarding.processor.CompanySetupProcessor;
import com.sme.be_sme.shared.gateway.core.BaseOperationFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OnboardingFacadeImpl extends BaseOperationFacade implements OnboardingFacade {

    private final CompanySetupProcessor companySetupProcessor;

    @Override
    public CompanySetupResponse setupCompany(CompanySetupRequest request) {
        return call(companySetupProcessor, request, CompanySetupResponse.class);
    }
}

