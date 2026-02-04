package com.sme.be_sme.shared.gateway.core;

import com.fasterxml.jackson.databind.JsonNode;

public abstract class BaseBizProcessor<C extends BizContext> implements OperationFacade {

    @Override
    public final Object execute(BizContext context) {
        BizContextHolder.set(context);
        try {
            @SuppressWarnings("unchecked")
            C typedContext = (C) context;
            if (skip(typedContext)) {
                return null;
            }

            preCheck(typedContext);
            Object result = doProcess(typedContext, context.getPayload());
            postProcess(typedContext, context.getPayload(), result);

            return result;
        } finally {
            BizContextHolder.clear();
        }
    }

    protected boolean skip(C context) { return false; }

    protected void preCheck(C context) {}

    protected void postProcess(C context, JsonNode payload, Object result) {}

    protected abstract Object doProcess(C context, JsonNode payload);
}
