package com.sme.be_sme.shared.security;

import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class OperationPermissionPolicy {

    private static final Set<String> PUBLIC_OPS = Set.of(
            "com.sme.identity.auth.login",
            "com.sme.onboarding.company.setup",
            "com.sme.company.register"
    );

    public boolean isPublic(String op) {
        return PUBLIC_OPS.contains(op);
    }

    // simplest: permission string == operationType
    public String requiredPermission(String op) {
        return op;
    }
}
