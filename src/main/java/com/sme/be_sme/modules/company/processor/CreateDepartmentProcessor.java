package com.sme.be_sme.modules.company.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.company.api.request.CreateDepartmentRequest;
import com.sme.be_sme.modules.company.api.response.CreateDepartmentResponse;
import com.sme.be_sme.modules.company.context.CreateDepartmentContext;
import com.sme.be_sme.shared.constant.ErrorCodes;
import com.sme.be_sme.shared.exception.AppException;
import com.sme.be_sme.shared.gateway.core.BaseCoreProcessor;
import com.sme.be_sme.shared.gateway.core.BizContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class CreateDepartmentProcessor extends BaseCoreProcessor<CreateDepartmentContext> {

    private final ObjectMapper objectMapper;
    private final CreateDepartmentCoreProcessor createDepartmentCoreProcessor;

    @Override
    protected CreateDepartmentContext buildContext(BizContext biz, JsonNode payload) {
        CreateDepartmentRequest req = objectMapper.convertValue(payload, CreateDepartmentRequest.class);

        CreateDepartmentContext ctx = new CreateDepartmentContext();
        ctx.setBiz(biz);
        ctx.setRequest(req);
        ctx.setResponse(new CreateDepartmentResponse());
        return ctx;
    }

    @Override
    @Transactional
    protected Object process(CreateDepartmentContext ctx) {
        validate(ctx);

        createDepartmentCoreProcessor.processWith(ctx);

        String departmentId = ctx.getDepartmentId();
        if (departmentId == null || departmentId.isBlank()) {
            throw AppException.of(ErrorCodes.INTERNAL_ERROR, "departmentId not set after create");
        }
        ctx.getResponse().setDepartmentId(departmentId);
        ctx.getResponse().setCompanyId(ctx.getBiz().getTenantId());
        ctx.getResponse().setName(ctx.getRequest().getName());
        ctx.getResponse().setManagerUserId(ctx.getRequest().getManagerId());
        return ctx.getResponse();
    }

    private static void validate(CreateDepartmentContext ctx) {
        if (ctx == null || ctx.getBiz() == null || ctx.getBiz().getTenantId() == null || ctx.getBiz().getTenantId().isBlank()) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "tenantId is required");
        }
        if (ctx.getRequest() == null) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "payload is required");
        }
        if (ctx.getRequest().getName() == null || ctx.getRequest().getName().isBlank()) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "name is required");
        }
        if (ctx.getRequest().getManagerId() == null || ctx.getRequest().getManagerId().isBlank()) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "Cannot create department: manager is required");
        }
    }
}

