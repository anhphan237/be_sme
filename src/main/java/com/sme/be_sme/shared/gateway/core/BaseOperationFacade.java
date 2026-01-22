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

        Object res = processor.execute(
                biz.getTenantId(),
                biz.getRequestId(),
                objectMapper.valueToTree(request)
        );

        return responseType.cast(res);
    }
}
