package com.sme.be_sme.shared.gateway.api;

import com.sme.be_sme.shared.api.BaseResponse;
import com.sme.be_sme.shared.gateway.core.OperationRouter;
import com.sme.be_sme.shared.security.GatewayAuthGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/gateway")
@RequiredArgsConstructor
public class OperationController {

    private final OperationRouter router;
    private final GatewayAuthGuard authGuard;

    @PostMapping
    public BaseResponse<Object> handle(@RequestBody OperationRequest req,
                                       @RequestHeader(value = "Authorization", required = false) String authorization) {

        String requestId = (req.getRequestId() != null && !req.getRequestId().isBlank())
                ? req.getRequestId()
                : UUID.randomUUID().toString();

        String tenantId = req.getTenantId();

        // ✅ AUTH CHECK tại gateway (đúng chỗ, vì đây mới có header)
        authGuard.check(req.getOperationType(), tenantId, authorization);

        Object data = router.route(req.getOperationType(), tenantId, requestId, req.getPayload());

        return BaseResponse.success(requestId, data);
    }
}
