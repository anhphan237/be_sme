package com.sme.be_sme.modules.identity.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.employee.processor.UpdateEmployeeProfileStatusCoreProcessor;
import com.sme.be_sme.modules.identity.api.request.DisableUserRequest;
import com.sme.be_sme.modules.identity.api.response.DisableUserResponse;
import com.sme.be_sme.modules.identity.context.IdentityUserDisableContext;
import com.sme.be_sme.shared.constant.ErrorCodes;
import com.sme.be_sme.shared.exception.AppException;
import com.sme.be_sme.shared.gateway.core.BaseCoreProcessor;
import com.sme.be_sme.shared.gateway.core.BizContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class IdentityUserDisableProcessor extends BaseCoreProcessor<IdentityUserDisableContext> {

    private final ObjectMapper objectMapper;

    private final DisableUserCoreProcessor disableUserCoreProcessor;
    private final UpdateEmployeeProfileStatusCoreProcessor updateEmployeeProfileStatusCoreProcessor;

    @Override
    protected IdentityUserDisableContext buildContext(BizContext biz, JsonNode payload) {
        DisableUserRequest req = objectMapper.convertValue(payload, DisableUserRequest.class);

        IdentityUserDisableContext ctx = new IdentityUserDisableContext();
        ctx.setBiz(biz);
        ctx.setRequest(req);
        ctx.setResponse(new DisableUserResponse());
        return ctx;
    }

    @Override
    @Transactional
    protected Object process(IdentityUserDisableContext ctx) {
        validate(ctx);

        String status = Boolean.TRUE.equals(ctx.getRequest().getDisabled()) ? "INACTIVE" : "ACTIVE";
        ctx.setStatus(status);

        disableUserCoreProcessor.processWith(ctx);
        updateEmployeeProfileStatusCoreProcessor.processWith(ctx);

        ctx.getResponse().setUserId(ctx.getRequest().getUserId());
        return ctx.getResponse();
    }

    private static void validate(IdentityUserDisableContext ctx) {
        if (ctx == null || ctx.getBiz() == null || ctx.getBiz().getTenantId() == null || ctx.getBiz().getTenantId().isBlank()) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "tenantId is required");
        }
        if (ctx.getRequest() == null) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "payload is required");
        }
        if (ctx.getRequest().getUserId() == null || ctx.getRequest().getUserId().isBlank()) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "userId is required");
        }
    }
}
