package com.sme.be_sme.modules.identity.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.identity.api.request.AssignRoleRequest;
import com.sme.be_sme.modules.identity.api.response.AssignRoleResponse;
import com.sme.be_sme.modules.identity.context.IdentityRoleAssignContext;
import com.sme.be_sme.shared.gateway.core.BaseCoreProcessor;
import com.sme.be_sme.shared.gateway.core.BizContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class IdentityRoleAssignProcessor extends BaseCoreProcessor<IdentityRoleAssignContext> {

    private final ObjectMapper objectMapper;

    private final ValidateRoleAssignCoreProcessor validateRoleAssignCoreProcessor;
    private final ResolveRoleIdByCodeCoreProcessor resolveRoleIdByCodeCoreProcessor;
    private final CheckUserRoleExistsCoreProcessor checkUserRoleExistsCoreProcessor;
    private final InsertUserRoleCoreProcessor insertUserRoleCoreProcessor;

    @Override
    protected IdentityRoleAssignContext buildContext(BizContext biz, JsonNode payload) {
        AssignRoleRequest req = objectMapper.convertValue(payload, AssignRoleRequest.class);

        IdentityRoleAssignContext ctx = new IdentityRoleAssignContext();
        ctx.setBiz(biz);
        ctx.setRequest(req);
        ctx.setResponse(new AssignRoleResponse());
        return ctx;
    }

    @Override
    @Transactional
    protected Object process(IdentityRoleAssignContext ctx) {
        // validate input + user exists/active
        validateRoleAssignCoreProcessor.processWith(ctx);

        // roleCode -> roleId
        resolveRoleIdByCodeCoreProcessor.processWith(ctx);

        // idempotent
        boolean existed = Boolean.TRUE.equals(checkUserRoleExistsCoreProcessor.processWith(ctx));
        if (!existed) {
            insertUserRoleCoreProcessor.processWith(ctx);
        }

        ctx.getResponse().setUserId(ctx.getRequest().getUserId());
        ctx.getResponse().setRoleCode(ctx.getRequest().getRoleCode());
        ctx.getResponse().setRoleId(ctx.getRoleId());
        return ctx.getResponse();
    }
}
