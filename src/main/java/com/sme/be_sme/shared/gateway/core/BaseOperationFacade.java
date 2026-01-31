package com.sme.be_sme.shared.gateway.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.shared.constant.ErrorCodes;
import com.sme.be_sme.shared.exception.AppException;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class BaseOperationFacade implements OperationFacadeProvider {

    @Autowired
    protected ObjectMapper objectMapper;

    protected <T> T call(BaseBizProcessor<?> processor, Object request, Class<T> responseType) {
        BizContext biz = BizContextHolder.get();
        if (biz == null) {
            throw AppException.of(ErrorCodes.INTERNAL_ERROR, "BizContext is missing");
        }

        // reuse context, only override payload for this call
        BizContext ctx = new BizContext();
        ctx.setRequestId(biz.getRequestId());
        ctx.setOperationType(biz.getOperationType());
        ctx.setTenantId(biz.getTenantId());
        ctx.setOperatorId(biz.getOperatorId());
        ctx.setRoles(biz.getRoles());
        ctx.setPayload(objectMapper.valueToTree(request));

        Object res = processor.execute(ctx);
        return responseType.cast(res);
    }
}
