package com.sme.be_sme.shared.security;

import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class PermissionService {

    public boolean allow(Set<String> roles, String requiredPerm) {
        if (roles.contains("ADMIN")) return true;

        // MVP hardcode tối thiểu để chạy
        // ví dụ HR_ADMIN được tạo user
        if (roles.contains("HR_ADMIN") && "com.sme.identity.user.create".equals(requiredPerm)) return true;

        return false;
    }
}