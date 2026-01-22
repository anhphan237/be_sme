package com.sme.be_sme.shared.gateway;

import com.sme.be_sme.shared.constant.ErrorCodes;
import com.sme.be_sme.shared.exception.AppException;
import com.sme.be_sme.shared.gateway.api.BaseRequest;
import com.sme.be_sme.shared.security.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class OperationGatewayAuth {

    private final JwtService jwtService;
    private final OperationPermissionPolicy policy;
    private final PermissionService permissionService;

    public void check(BaseRequest req, String authorizationHeader) {
        String op = req.getOperationType();
        if (policy.isPublic(op)) return;

        if (!StringUtils.hasText(req.getTenantId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "tenantId is required");
        }

        String token = extractBearer(authorizationHeader);
        JwtPrincipal p;
        try {
            p = jwtService.verify(token);
        } catch (SecurityException se) {
            throw AppException.of(ErrorCodes.UNAUTHORIZED, se.getMessage());
        }

        // tenant match (multi-tenant hard guard)
        if (!req.getTenantId().equals(p.getTenantId())) {
            throw AppException.of(ErrorCodes.FORBIDDEN, "tenantId mismatch");
        }

        String requiredPerm = policy.requiredPermission(op);
        if (!permissionService.allow(p.getRoles(), requiredPerm)) {
            throw AppException.of(ErrorCodes.FORBIDDEN, "no permission");
        }

        // nếu muốn: set ThreadLocal context ở đây
        // AuthContextHolder.set(...)
    }

    private String extractBearer(String header) {
        if (!StringUtils.hasText(header) || !header.startsWith("Bearer ")) {
            throw AppException.of(ErrorCodes.UNAUTHORIZED, "Missing bearer token");
        }
        String token = header.substring("Bearer ".length()).trim();
        if (!StringUtils.hasText(token)) {
            throw AppException.of(ErrorCodes.UNAUTHORIZED, "Missing bearer token");
        }
        return token;
    }
}
