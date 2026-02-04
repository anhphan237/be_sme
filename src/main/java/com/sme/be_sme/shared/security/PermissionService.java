package com.sme.be_sme.shared.security;

import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class PermissionService {

    public boolean allow(Set<String> roles, String requiredPerm) {
        if (roles.contains("ADMIN")) return true;

        // ví dụ HR/HR_ADMIN được tạo user
        if ("com.sme.identity.user.create".equals(requiredPerm)
                && (roles.contains("HR") || roles.contains("HR_ADMIN"))) {
            return true;
        }

        return false;
    }
}