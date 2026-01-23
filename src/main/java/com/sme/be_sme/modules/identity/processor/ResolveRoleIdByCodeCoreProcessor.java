package com.sme.be_sme.modules.identity.processor;

import com.sme.be_sme.modules.identity.context.IdentityRoleAssignContext;
import com.sme.be_sme.modules.identity.infrastructure.mapper.RoleMapperExt;
import com.sme.be_sme.shared.constant.ErrorCodes;
import com.sme.be_sme.shared.exception.AppException;
import com.sme.be_sme.shared.gateway.core.BaseCoreProcessor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ResolveRoleIdByCodeCoreProcessor extends BaseCoreProcessor<IdentityRoleAssignContext> {

    private final RoleMapperExt roleMapperExt;

    @Override
    protected Object process(IdentityRoleAssignContext ctx) {
        String roleId = roleMapperExt.selectRoleIdByCompanyIdAndCode(
                ctx.getBiz().getTenantId(),
                ctx.getRequest().getRoleCode()
        );

        if (roleId == null || roleId.isBlank()) {
            throw AppException.of(ErrorCodes.NOT_FOUND, "role not found");
        }

        ctx.setRoleId(roleId);
        return null;
    }
}
