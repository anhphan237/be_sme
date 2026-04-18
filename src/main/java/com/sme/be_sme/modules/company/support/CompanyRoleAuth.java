package com.sme.be_sme.modules.company.support;

import java.util.Set;

public final class CompanyRoleAuth {
    private CompanyRoleAuth() {}

    public static boolean isHrOnly(Set<String> roles) {
        if (roles == null || roles.isEmpty()) {
            return false;
        }
        for (String role : roles) {
            if (role == null) {
                continue;
            }
            String upper = role.trim().toUpperCase();
            if ("HR".equals(upper) || "HR_ADMIN".equals(upper)) {
                return true;
            }
        }
        return false;
    }
}
