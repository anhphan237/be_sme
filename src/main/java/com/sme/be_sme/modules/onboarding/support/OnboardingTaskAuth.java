package com.sme.be_sme.modules.onboarding.support;

import java.util.Set;
import java.util.stream.Collectors;

public final class OnboardingTaskAuth {

    private OnboardingTaskAuth() {}

    public static boolean isHrManagerAdmin(Set<String> roles) {
        if (roles == null || roles.isEmpty()) {
            return false;
        }
        Set<String> upper = roles.stream()
                .map(r -> r != null ? r.trim().toUpperCase() : "")
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
        return upper.contains("HR") || upper.contains("MANAGER");
    }

    /**
     * User is only EMPLOYEE (no elevated onboarding role).
     */
    public static boolean isEmployeeOnly(Set<String> roles) {
        if (roles == null || roles.isEmpty()) {
            return false;
        }
        Set<String> upper = roles.stream()
                .map(r -> r != null ? r.trim().toUpperCase() : "")
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
        if (!upper.contains("EMPLOYEE")) {
            return false;
        }
        return !isHrManagerAdmin(roles);
    }

    /**
     * IT-only (no EMPLOYEE role): must be scoped to tasks where they are assignee.
     * IT+EMPLOYEE is handled by {@link #isEmployeeOnly}; IT+MANAGER/HR uses elevated rules.
     */
    public static boolean isItStaffScopedToAssignee(Set<String> roles) {
        if (roles == null || roles.isEmpty()) {
            return false;
        }
        Set<String> upper = roles.stream()
                .map(r -> r != null ? r.trim().toUpperCase() : "")
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
        if (upper.contains("EMPLOYEE")) {
            return false;
        }
        return upper.contains("IT") && !isHrManagerAdmin(roles);
    }
}
