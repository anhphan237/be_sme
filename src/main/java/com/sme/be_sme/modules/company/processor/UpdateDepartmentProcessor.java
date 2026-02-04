package com.sme.be_sme.modules.company.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.company.api.request.UpdateDepartmentRequest;
import com.sme.be_sme.modules.company.api.response.UpdateDepartmentResponse;
import com.sme.be_sme.modules.company.context.UpdateDepartmentContext;
import com.sme.be_sme.shared.constant.ErrorCodes;
import com.sme.be_sme.shared.exception.AppException;
import com.sme.be_sme.shared.gateway.core.BaseCoreProcessor;
import com.sme.be_sme.shared.gateway.core.BizContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class UpdateDepartmentProcessor extends BaseCoreProcessor<UpdateDepartmentContext> {

    private final ObjectMapper objectMapper;

    private final UpdateDepartmentCoreProcessor updateDepartmentCoreProcessor;

    @Override
    protected UpdateDepartmentContext buildContext(BizContext biz, JsonNode payload) {
        UpdateDepartmentRequest req = objectMapper.convertValue(payload, UpdateDepartmentRequest.class);

        UpdateDepartmentContext ctx = new UpdateDepartmentContext();
        ctx.setBiz(biz);
        ctx.setRequest(req);
        ctx.setResponse(new UpdateDepartmentResponse());
        return ctx;
    }

    @Override
    @Transactional
    protected Object process(UpdateDepartmentContext ctx) {
        validate(ctx);

        updateDepartmentCoreProcessor.processWith(ctx);

        ctx.getResponse().setDepartmentId(ctx.getRequest().getDepartmentId());
        ctx.getResponse().setCompanyId(ctx.getBiz().getTenantId());
        ctx.getResponse().setName(ctx.getRequest().getName());   // nếu FE không gửi name thì vẫn trả null
        ctx.getResponse().setStatus(ctx.getRequest().getStatus());
        return ctx.getResponse();
    }

    private static void validate(UpdateDepartmentContext ctx) {
        if (ctx == null || ctx.getBiz() == null || ctx.getBiz().getTenantId() == null || ctx.getBiz().getTenantId().isBlank()) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "tenantId is required");
        }
        if (ctx.getRequest() == null) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "payload is required");
        }
        if (ctx.getRequest().getCompanyId() == null || ctx.getRequest().getCompanyId().isBlank()) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "companyId is required");
        }
        if (!ctx.getBiz().getTenantId().equals(ctx.getRequest().getCompanyId())) {
            throw AppException.of(ErrorCodes.FORBIDDEN, "companyId mismatch");
        }
        if (ctx.getRequest().getDepartmentId() == null || ctx.getRequest().getDepartmentId().isBlank()) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "departmentId is required");
        }

        // ít nhất phải có 1 field để update
        boolean hasUpdate =
                (ctx.getRequest().getName() != null && !ctx.getRequest().getName().isBlank())
                        || (ctx.getRequest().getType() != null && !ctx.getRequest().getType().isBlank())
                        || (ctx.getRequest().getStatus() != null && !ctx.getRequest().getStatus().isBlank());

        if (!hasUpdate) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "no fields to update");
        }
    }
}
