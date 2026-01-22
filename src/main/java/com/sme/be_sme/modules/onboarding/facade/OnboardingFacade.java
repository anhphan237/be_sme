package com.sme.be_sme.modules.onboarding.facade;

import com.sme.be_sme.modules.onboarding.api.request.CompanySetupRequest;
import com.sme.be_sme.modules.onboarding.api.response.CompanySetupResponse;
import com.sme.be_sme.shared.gateway.annotation.OperationType;
import com.sme.be_sme.shared.gateway.core.OperationFacadeProvider;

public interface OnboardingFacade extends OperationFacadeProvider {
    @OperationType("com.sme.onboarding.company.setup")
    CompanySetupResponse setupCompany(CompanySetupRequest request);
}
