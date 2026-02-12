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

        UpdateDepartmentResponse res = ctx.getResponse();
        res.setDepartmentId(ctx.getDepartmentId());
        res.setCompanyId(ctx.getBiz().getTenantId());
        res.setName(ctx.getName());
        res.setType(ctx.getType());
        res.setStatus(ctx.getStatus());
        res.setManagerUserId(ctx.getManagerUserId());
        return res;
    }

    private static void validate(UpdateDepartmentContext ctx) {
        if (ctx == null || ctx.getBiz() == null || ctx.getBiz().getTenantId() == null || ctx.getBiz().getTenantId().isBlank()) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "tenantId is required");
        }
        if (ctx.getRequest() == null) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "payload is required");
        }
        if (ctx.getRequest().getDepartmentId() == null || ctx.getRequest().getDepartmentId().isBlank()) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "departmentId is required");
        }
        // giống create: bắt buộc name
        if (ctx.getRequest().getName() == null || ctx.getRequest().getName().isBlank()) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "name is required");
        }
    }
}
