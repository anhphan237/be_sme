package com.sme.be_sme.modules.onboarding.processor;

import com.sme.be_sme.modules.company.context.CompanySetupContext;
import com.sme.be_sme.modules.identity.api.request.AssignRoleRequest;
import com.sme.be_sme.modules.identity.processor.AssignRoleProcessor;
import com.sme.be_sme.shared.gateway.core.BaseCoreProcessor;
import com.sme.be_sme.shared.gateway.core.BizContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@RequiredArgsConstructor
public class CompanySetupAssignAdminRoleCoreProcessor extends BaseCoreProcessor<CompanySetupContext> {

    private static final String ADMIN_ROLE_CODE = "ADMIN";
    private final AssignRoleProcessor assignRoleProcessor;

    @Override
    protected Object process(CompanySetupContext ctx) {
        BizContext tenantCtx = BizContext.internal(
                ctx.getCompany().getCompanyId(),
                ctx.getBiz() != null ? ctx.getBiz().getRequestId() : null,
                ctx.getAdminUser().getUserId(),   
                Set.of("ADMIN")
        );

        AssignRoleRequest r = new AssignRoleRequest();
        r.setUserId(ctx.getAdminUser().getUserId());
        r.setRoleCode(ADMIN_ROLE_CODE);

        assignRoleProcessor.process(tenantCtx, r);
        return null;
    }
}

