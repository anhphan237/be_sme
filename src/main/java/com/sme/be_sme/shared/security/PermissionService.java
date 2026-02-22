package com.sme.be_sme.shared.security;

import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class PermissionService {

    /** Platform-only permissions: not granted to company HR. */
    private static final String PLATFORM_PREFIX = "com.sme.analytics.platform.";

    public boolean allow(Set<String> roles, String requiredPerm) {
        if (roles == null) return false;

        if (roles.contains("ADMIN")) return true;

        // Platform-only: reserved for platform admin (e.g. subscription metrics)
        if (requiredPerm != null && requiredPerm.startsWith(PLATFORM_PREFIX)) {
            return false;
        }

        // HR: almost all company (tenant) permissions
        if ((roles.contains("HR") || roles.contains("HR_ADMIN"))
                && requiredPerm != null && requiredPerm.startsWith("com.sme.")) {
            return true;
        }

        // user.list: STAFF (platform) can view list
        if ("com.sme.identity.user.list".equals(requiredPerm) && roles.contains("STAFF")) {
            return true;
        }

        // survey response submit: any authenticated user (e.g. employee filling survey)
        if ("com.sme.survey.response.submit".equals(requiredPerm) && !roles.isEmpty()) {
            return true;
        }

        return false;
    }
}