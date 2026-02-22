package com.sme.be_sme.shared.security;

import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

@Component
public class PermissionService {

    /** Platform-only permissions: not granted to company HR. */
    private static final String PLATFORM_PREFIX = "com.sme.analytics.platform.";

    public boolean allow(Set<String> roles, String requiredPerm) {
        if (roles == null) return false;

        Set<String> rolesUpper = roles.stream()
                .map(r -> r != null ? r.trim().toUpperCase() : "")
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());

        if (rolesUpper.contains("ADMIN")) return true;

        // Platform-only: reserved for platform admin (e.g. subscription metrics)
        if (requiredPerm != null && requiredPerm.startsWith(PLATFORM_PREFIX)) {
            return false;
        }

        // HR / MANAGER: almost all company (tenant) permissions
        if ((rolesUpper.contains("HR") || rolesUpper.contains("HR_ADMIN") || rolesUpper.contains("MANAGER"))
                && requiredPerm != null && requiredPerm.startsWith("com.sme.")) {
            return true;
        }

        // user.list: STAFF (platform) can view list
        if ("com.sme.identity.user.list".equals(requiredPerm) && rolesUpper.contains("STAFF")) {
            return true;
        }

        // survey response submit: any authenticated user (e.g. employee filling survey)
        if ("com.sme.survey.response.submit".equals(requiredPerm) && !rolesUpper.isEmpty()) {
            return true;
        }

        // EMPLOYEE: task page, onboarding view, notifications, documents, own profile
        if (rolesUpper.contains("EMPLOYEE") && requiredPerm != null) {
            String perm = requiredPerm.trim();
            if ("com.sme.onboarding.task.listByOnboarding".equalsIgnoreCase(perm)
                    || "com.sme.onboarding.task.updateStatus".equalsIgnoreCase(perm)
                    || "com.sme.onboarding.instance.list".equalsIgnoreCase(perm)
                    || "com.sme.onboarding.instance.get".equalsIgnoreCase(perm)
                    || "com.sme.notification.list".equalsIgnoreCase(perm)
                    || "com.sme.notification.markRead".equalsIgnoreCase(perm)
                    || "com.sme.content.document.list".equalsIgnoreCase(perm)
                    || "com.sme.content.document.acknowledge".equalsIgnoreCase(perm)
                    || "com.sme.identity.user.get".equalsIgnoreCase(perm)) {
                return true;
            }
        }

        return false;
    }
}