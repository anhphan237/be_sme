package com.sme.be_sme.shared.security;

import lombok.Getter;

import java.util.Set;

@Getter
public class JwtPrincipal {
    private final String userId;
    private final String tenantId;
    private final Set<String> roles;

    public JwtPrincipal(String userId, String tenantId, Set<String> roles) {
        this.userId = userId;
        this.tenantId = tenantId;
        this.roles = roles;
    }
}
