package com.sme.be_sme.modules.company.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.company.api.request.ListDepartmentRequest;
import com.sme.be_sme.modules.company.api.response.ListDepartmentResponse;
import com.sme.be_sme.modules.company.context.ListDepartmentContext;
import com.sme.be_sme.shared.constant.ErrorCodes;
import com.sme.be_sme.shared.exception.AppException;
import com.sme.be_sme.shared.gateway.core.BaseCoreProcessor;
import com.sme.be_sme.shared.gateway.core.BizContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ListDepartmentProcessor extends BaseCoreProcessor<ListDepartmentContext> {

    private final ObjectMapper objectMapper;
    private final ListDepartmentCoreProcessor listDepartmentCoreProcessor;

    @Override
    protected ListDepartmentContext buildContext(BizContext biz, JsonNode payload) {
        ListDepartmentRequest req =
                payload == null ? new ListDepartmentRequest()
                        : objectMapper.convertValue(payload, ListDepartmentRequest.class);

        ListDepartmentContext ctx = new ListDepartmentContext();
        ctx.setBiz(biz);
        ctx.setRequest(req);
        ctx.setResponse(new ListDepartmentResponse());
        return ctx;
    }

    @Override
    protected Object process(ListDepartmentContext ctx) {
        validate(ctx);

        listDepartmentCoreProcessor.processWith(ctx);

        ctx.getResponse().setItems(ctx.getItems());
        return ctx.getResponse();
    }

    private static void validate(ListDepartmentContext ctx) {
        if (ctx == null || ctx.getBiz() == null
                || ctx.getBiz().getTenantId() == null
                || ctx.getBiz().getTenantId().isBlank()) {
            throw AppException.of(ErrorCodes.UNAUTHORIZED, "tenantId is required");
        }
    }
}
