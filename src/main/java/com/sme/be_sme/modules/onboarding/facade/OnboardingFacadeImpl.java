package com.sme.be_sme.modules.onboarding.facade;

import com.sme.be_sme.modules.onboarding.api.request.CompanySetupRequest;
import com.sme.be_sme.modules.onboarding.api.response.CompanySetupResponse;
import com.sme.be_sme.modules.onboarding.processor.CompanySetupProcessor;
import com.sme.be_sme.shared.gateway.core.BizContextHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OnboardingFacadeImpl implements OnboardingFacade {

    private final CompanySetupProcessor processor;

    @Override
    public CompanySetupResponse setupCompany(CompanySetupRequest request) {
        return processor.process(BizContextHolder.get(), request);
    }
}
