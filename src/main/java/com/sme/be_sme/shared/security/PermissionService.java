package com.sme.be_sme.shared.security;

import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class PermissionService {

    public boolean allow(Set<String> roles, String requiredPerm) {
        if (roles.contains("ADMIN")) return true;

        // HR/HR_ADMIN: create user
        if ("com.sme.identity.user.create".equals(requiredPerm)
                && (roles.contains("HR") || roles.contains("HR_ADMIN"))) {
            return true;
        }

        // user.list: only ADMIN, STAFF (platform), HR (tenant) can view list
        if ("com.sme.identity.user.list".equals(requiredPerm)
                && (roles.contains("HR") || roles.contains("HR_ADMIN") || roles.contains("STAFF"))) {
            return true;
        }

        // onboarding/survey template create: HR (tenant) can create templates
        if (("com.sme.onboarding.template.create".equals(requiredPerm) || "com.sme.survey.template.create".equals(requiredPerm))
                && (roles.contains("HR") || roles.contains("HR_ADMIN"))) {
            return true;
        }

        return false;
    }
}