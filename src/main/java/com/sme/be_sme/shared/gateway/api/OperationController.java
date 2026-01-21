package com.sme.be_sme.shared.gateway.api;

import com.sme.be_sme.shared.api.BaseResponse;
import com.sme.be_sme.shared.gateway.core.OperationRouter;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/gateway")
@RequiredArgsConstructor
public class OperationController {

    private final OperationRouter router;

    @PostMapping
    public BaseResponse<Object> handle(@RequestBody OperationRequest req) {

        String requestId = (req.getRequestId() != null && !req.getRequestId().isBlank())
                ? req.getRequestId()
                : UUID.randomUUID().toString();

        String tenantId = req.getTenantId();

        Object data = router.route(req.getOperationType(), tenantId, requestId, req.getPayload());

        return BaseResponse.success(requestId, data);
    }
}
