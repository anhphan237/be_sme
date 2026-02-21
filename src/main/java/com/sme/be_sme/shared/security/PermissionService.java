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

        // onboarding/survey template list & get: HR (tenant) can list and get templates
        if ((requiredPerm != null && (requiredPerm.equals("com.sme.onboarding.template.list")
                || requiredPerm.equals("com.sme.onboarding.template.get")
                || requiredPerm.equals("com.sme.survey.template.list")
                || requiredPerm.equals("com.sme.survey.template.get")))
                && (roles.contains("HR") || roles.contains("HR_ADMIN"))) {
            return true;
        }

        // survey: HR (tenant) can manage templates, instances, questions, responses, reports
        if (requiredPerm != null && requiredPerm.startsWith("com.sme.survey.")
                && (roles.contains("HR") || roles.contains("HR_ADMIN"))) {
            return true;
        }

        // survey response submit: any authenticated user (e.g. employee filling survey)
        if ("com.sme.survey.response.submit".equals(requiredPerm) && roles != null && !roles.isEmpty()) {
            return true;
        }

        return false;
    }
}