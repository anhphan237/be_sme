package com.sme.be_sme.shared.gateway.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.InvalidDefinitionException;
import com.sme.be_sme.shared.constant.ErrorCodes;
import com.sme.be_sme.shared.exception.AppException;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.function.Supplier;

public abstract class BaseOperationFacade implements OperationFacadeProvider {

    @Autowired
    protected ObjectMapper objectMapper;

//    protected <T> T call(BaseBizProcessor<?> processor, Object request, Class<T> responseType) {
//        BizContext biz = BizContextHolder.get();
//        if (biz == null) {
//            throw AppException.of(ErrorCodes.INTERNAL_ERROR, "BizContext is missing");
//        }
//
//        // reuse context, only override payload for this call
//        BizContext ctx = new BizContext();
//        ctx.setRequestId(biz.getRequestId());
//        ctx.setOperationType(biz.getOperationType());
//        ctx.setTenantId(biz.getTenantId());
//        ctx.setOperatorId(biz.getOperatorId());
//        ctx.setRoles(biz.getRoles());
//        ctx.setPayload(objectMapper.valueToTree(request));
//
//        Object res = processor.execute(ctx);
//        return responseType.cast(res);
//    }

    // 1. Hàm cũ (giữ lại để tương thích ngược, mặc định dùng BizContext)
    protected <T> T call(BaseBizProcessor<BizContext> processor, Object request, Class<T> responseType) {
        return call(processor, request, responseType, BizContext::new);
    }

    // 2. Hàm mới (OVERLOAD - nhận thêm Supplier<C>)
    protected <T, C extends BizContext> T call(
            BaseBizProcessor<C> processor,
            Object request,
            Class<T> responseType,
            Supplier<C> contextSupplier // <--- Tham số quan trọng nhất
    ) {
        BizContext biz = BizContextHolder.get();
        if (biz == null) {
            throw AppException.of(ErrorCodes.INTERNAL_ERROR, "BizContext is missing");
        }

        // --- KEY FIX: Dùng supplier để tạo đúng class con (IdentityUpdateUserContext) ---
        C ctx = contextSupplier.get();

        // Copy dữ liệu từ cha sang con
        ctx.setRequestId(biz.getRequestId());
        ctx.setOperationType(biz.getOperationType());
        ctx.setTenantId(biz.getTenantId());
        ctx.setOperatorId(biz.getOperatorId());
        ctx.setRoles(biz.getRoles());

        // Copy payload. Some request DTOs are intentionally empty (e.g. list-all APIs).
        // Jackson may throw FAIL_ON_EMPTY_BEANS during valueToTree, so fallback to {}.
        ctx.setPayload(toPayloadNode(request));

        // Gọi execute
        Object res = processor.execute(ctx);
        return responseType.cast(res);
    }

    private JsonNode toPayloadNode(Object request) {
        try {
            return objectMapper.valueToTree(request);
        } catch (IllegalArgumentException ex) {
            Throwable cause = ex.getCause();
            if (cause instanceof InvalidDefinitionException) {
                return objectMapper.createObjectNode();
            }
            throw ex;
        }
    }
}
