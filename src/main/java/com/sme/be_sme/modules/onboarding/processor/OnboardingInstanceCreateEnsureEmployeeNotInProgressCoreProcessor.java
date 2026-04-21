package com.sme.be_sme.modules.onboarding.processor;

import com.sme.be_sme.modules.onboarding.context.OnboardingInstanceCreateContext;
import com.sme.be_sme.modules.onboarding.infrastructure.mapper.OnboardingInstanceMapper;
import com.sme.be_sme.modules.onboarding.infrastructure.persistence.entity.OnboardingInstanceEntity;
import com.sme.be_sme.shared.constant.ErrorCodes;
import com.sme.be_sme.shared.exception.AppException;
import com.sme.be_sme.shared.gateway.core.BaseCoreProcessor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OnboardingInstanceCreateEnsureEmployeeNotInProgressCoreProcessor
        extends BaseCoreProcessor<OnboardingInstanceCreateContext> {

    private final OnboardingInstanceMapper onboardingInstanceMapper;

    @Override
    protected Object process(OnboardingInstanceCreateContext ctx) {
        if (ctx.getExistingInstance() != null) {
            return null;
        }

        String companyId = ctx.getBiz().getTenantId();
        String employeeId = ctx.getRequest().getEmployeeId().trim();
        OnboardingInstanceEntity inProgress = onboardingInstanceMapper.selectInProgressByCompanyIdAndEmployeeId(
                companyId, employeeId);
        if (inProgress != null) {
            throw AppException.of(
                    ErrorCodes.DUPLICATED,
                    "người này đang onboard rồi nên không thêm được onboard nữa");
        }
        return null;
    }
}
