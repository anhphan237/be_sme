package com.sme.be_sme.modules.identity.processor;

import com.sme.be_sme.modules.identity.context.IdentityRoleAssignContext;
import com.sme.be_sme.modules.identity.infrastructure.mapper.UserRoleMapperExt;
import com.sme.be_sme.shared.gateway.core.BaseCoreProcessor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CheckRoleAssignedCoreProcessor extends BaseCoreProcessor<IdentityRoleAssignContext> {

    private final UserRoleMapperExt userRoleMapperExt;

    @Override
    protected Object process(IdentityRoleAssignContext ctx) {
        int cnt = userRoleMapperExt.countByCompanyIdAndUserIdAndRoleId(
                ctx.getBiz().getTenantId(),
                ctx.getRequest().getUserId(),
                ctx.getRequest().getRoleCode()
        );
        return cnt > 0;
    }
}
