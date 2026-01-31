package com.sme.be_sme.shared.gateway.annotation;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OperationHandler {
    String value(); // operationType: com.sme.xxx....
}
