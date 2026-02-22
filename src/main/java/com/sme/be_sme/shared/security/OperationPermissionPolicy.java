package com.sme.be_sme.shared.security;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Set;

@Component
public class OperationPermissionPolicy {

    private static final Set<String> PUBLIC_OPS = Set.of(
            "com.sme.identity.auth.login",
            "com.sme.identity.auth.checkEmailExists",
            "com.sme.onboarding.company.setup",
            "com.sme.company.register",
            // Allow task list for employees (also checked in PermissionService for EMPLOYEE)
            "com.sme.onboarding.task.listByOnboarding"
    );

    public boolean isPublic(String op) {
        if (!StringUtils.hasText(op)) return false;
        String normalized = op.trim();
        if (PUBLIC_OPS.contains(normalized)) return true;
        // Allow case-insensitive match so FE typos (e.g. ListByOnboarding) still work
        return PUBLIC_OPS.stream().anyMatch(p -> p.equalsIgnoreCase(normalized));
    }

    // simplest: permission string == operationType (normalized)
    public String requiredPermission(String op) {
        return StringUtils.hasText(op) ? op.trim() : op;
    }
}
