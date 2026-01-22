package com.sme.be_sme.shared.gateway.core;

public final class BizContextHolder {
    private static final ThreadLocal<BizContext> HOLDER = new ThreadLocal<>();

    private BizContextHolder() {}

    public static void set(BizContext context) {
        HOLDER.set(context);
    }

    public static BizContext get() {
        return HOLDER.get();
    }

    public static void clear() {
        HOLDER.remove();
    }
}
