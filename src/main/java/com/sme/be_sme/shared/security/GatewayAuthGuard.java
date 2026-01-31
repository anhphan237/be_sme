package com.sme.be_sme.shared.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.sme.be_sme.shared.constant.ErrorCodes;
import com.sme.be_sme.shared.exception.AppException;
import com.sme.be_sme.shared.gateway.core.BizContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Set;

@Component
@RequiredArgsConstructor
public class GatewayAuthGuard {

    private final JwtService jwtService;
    private final OperationPermissionPolicy policy;
    private final PermissionService permissionService;

    public BizContext buildContext(
            String operationType,
            String requestId,
            JsonNode payload,
            String authorizationHeader
    ) {
        BizContext ctx = BizContext.of(requestId, operationType, payload);

        // public operation
        if (policy.isPublic(operationType)) {
            ctx.setRoles(Set.of());
            return ctx;
        }

        String token = extractBearer(authorizationHeader);

        JwtPrincipal principal;
        try {
            principal = jwtService.verify(token);
        } catch (SecurityException e) {
            throw AppException.of(ErrorCodes.UNAUTHORIZED, e.getMessage());
        }

        // tenant/operator/roles ONLY from JWT
        ctx.setTenantId(principal.getTenantId());
        ctx.setOperatorId(principal.getUserId());
        ctx.setRoles(principal.getRoles());

        // permission check (giữ nguyên)
        String requiredPerm = policy.requiredPermission(operationType);
        if (!permissionService.allow(principal.getRoles(), requiredPerm)) {
            throw AppException.of(ErrorCodes.FORBIDDEN, "no permission");
        }

        return ctx;
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
