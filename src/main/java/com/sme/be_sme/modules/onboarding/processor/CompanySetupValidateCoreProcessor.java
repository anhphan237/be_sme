package com.sme.be_sme.modules.onboarding.processor;

import com.sme.be_sme.modules.company.context.CompanySetupContext;
import com.sme.be_sme.shared.constant.ErrorCodes;
import com.sme.be_sme.shared.exception.AppException;
import com.sme.be_sme.shared.gateway.core.BaseCoreProcessor;
import org.springframework.stereotype.Component;

@Component
public class CompanySetupValidateCoreProcessor extends BaseCoreProcessor<CompanySetupContext> {
    @Override
    protected Object process(CompanySetupContext ctx) {
        if (ctx.getRequest() == null || ctx.getRequest().getCompany() == null) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "company is required");
        }
        if (ctx.getRequest().getDepartment() == null) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "department is required");
        }
        if (ctx.getRequest().getAdminUser() == null) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "adminUser is required");
        }
        return null;
    }
}

