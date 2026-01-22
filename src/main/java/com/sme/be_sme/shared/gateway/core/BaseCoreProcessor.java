package com.sme.be_sme.shared.gateway.core;

import com.fasterxml.jackson.databind.JsonNode;

public abstract class BaseCoreProcessor<C> extends BaseBizProcessor<BizContext> {

    @Override
    protected final Object doProcess(BizContext biz, JsonNode payload) {
        C ctx = buildContext(biz, payload);
        return process(ctx);
    }

    /**
     * If a processor is internal-only, it can return null here and rely on processWith(ctx).
     * But if it is exposed as an operation, it must build non-null context.
     */
    protected C buildContext(BizContext biz, JsonNode payload) {
        return null;
    }

    protected abstract Object process(C ctx);

    public final Object processWith(C ctx) {
        return process(ctx);
    }
}

