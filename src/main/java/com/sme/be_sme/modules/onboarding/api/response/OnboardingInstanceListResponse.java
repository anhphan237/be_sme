package com.sme.be_sme.modules.onboarding.api.response;

import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OnboardingInstanceListResponse {
    private List<OnboardingInstanceDetailResponse> instances;
}
