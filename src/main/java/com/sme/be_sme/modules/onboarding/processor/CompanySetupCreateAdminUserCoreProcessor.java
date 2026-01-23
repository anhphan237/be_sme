package com.sme.be_sme.modules.onboarding.processor;

import com.sme.be_sme.modules.identity.api.request.CreateUserRequest;
import com.sme.be_sme.modules.identity.api.response.CreateUserResponse;
import com.sme.be_sme.modules.identity.processor.CreateUserProcessor;
import com.sme.be_sme.modules.company.context.CompanySetupContext;
import com.sme.be_sme.shared.constant.ErrorCodes;
import com.sme.be_sme.shared.exception.AppException;
import com.sme.be_sme.shared.gateway.core.BaseCoreProcessor;
import com.sme.be_sme.shared.gateway.core.BizContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CompanySetupCreateAdminUserCoreProcessor extends BaseCoreProcessor<CompanySetupContext> {

    private final CreateUserProcessor createUserProcessor;

    @Override
    protected Object process(CompanySetupContext ctx) {
        if (ctx.getCompany() == null || ctx.getCompany().getCompanyId() == null || ctx.getCompany().getCompanyId().isBlank()) {
            throw AppException.of(ErrorCodes.INTERNAL_ERROR, "companyId missing after createCompany");
        }
        if (ctx.getRequest() == null || ctx.getRequest().getAdminUser() == null) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "adminUser is required");
        }

        String companyId = ctx.getCompany().getCompanyId();
        String requestId = (ctx.getBiz() != null) ? ctx.getBiz().getRequestId() : null;
        BizContext tenantCtx = BizContext.of(companyId, requestId);

        // enforce companyId in admin user request (nếu request có field companyId)
        CreateUserRequest adminReq = ctx.getRequest().getAdminUser();
        // adminReq.setCompanyId(companyId); // bật dòng này nếu CreateUserRequest có companyId

        CreateUserResponse adminUser = createUserProcessor.process(tenantCtx, adminReq);
        ctx.setAdminUser(adminUser);
        return null;
    }
}

