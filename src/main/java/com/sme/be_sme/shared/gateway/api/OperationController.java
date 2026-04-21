package com.sme.be_sme.shared.gateway.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.sme.be_sme.modules.platform.service.PlatformErrorLogSupport;
import com.sme.be_sme.shared.api.BaseResponse;
import com.sme.be_sme.shared.gateway.core.BizContext;
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
    private final PlatformErrorLogSupport platformErrorLogSupport;

    @PostMapping
    public BaseResponse<Object> handle(@RequestBody OperationRequest req,
                                       @RequestHeader(value = "Authorization", required = false) String authorization) {

        String requestId = (req != null && req.getRequestId() != null && !req.getRequestId().isBlank())
                ? req.getRequestId()
                : UUID.randomUUID().toString();

        String operationType = req == null ? null : req.getOperationType();
        JsonNode payload = req == null ? null : req.getPayload();

        BizContext ctx = null;

        try {
            ctx = authGuard.buildContext(
                    operationType,
                    requestId,
                    payload,
                    authorization
            );

            Object data = router.route(ctx);

            return BaseResponse.success(requestId, data);
        } catch (Exception ex) {
            platformErrorLogSupport.log(
                    ctx,
                    requestId,
                    operationType,
                    payload,
                    ex
            );

            throw ex;
        }
    }
}
