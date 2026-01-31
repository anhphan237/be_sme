package com.sme.be_sme.shared.gateway.core;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OperationRouter {

    private final FacadeRegistry registry;

    public Object route(BizContext context) {
        return registry.get(context.getOperationType()).execute(context);
    }
}
