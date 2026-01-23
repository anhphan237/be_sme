package com.sme.be_sme.shared.security;

import com.sme.be_sme.shared.constant.ErrorCodes;
import com.sme.be_sme.shared.exception.AppException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class GatewayAuthGuard {

    private final JwtService jwtService;
    private final OperationPermissionPolicy policy;
    private final PermissionService permissionService;

    public void check(String operationType, String tenantId, String authorizationHeader) {
        if (policy.isPublic(operationType)) return;

        String token = extractBearer(authorizationHeader);

        JwtPrincipal principal;
        try {
            principal = jwtService.verify(token);
        } catch (SecurityException e) {
            throw AppException.of(ErrorCodes.UNAUTHORIZED, e.getMessage());
        }

        if (!StringUtils.hasText(tenantId) || !tenantId.equals(principal.getTenantId())) {
            throw AppException.of(ErrorCodes.FORBIDDEN, "tenantId mismatch");
        }

        String requiredPerm = policy.requiredPermission(operationType);
        if (!permissionService.allow(principal.getRoles(), requiredPerm)) {
            throw AppException.of(ErrorCodes.FORBIDDEN, "no permission");
        }
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
