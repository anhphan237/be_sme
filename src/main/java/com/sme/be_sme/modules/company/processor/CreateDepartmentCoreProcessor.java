package com.sme.be_sme.modules.company.processor;

import com.sme.be_sme.modules.company.context.CreateDepartmentContext;
import com.sme.be_sme.modules.company.infrastructure.mapper.DepartmentMapper;
import com.sme.be_sme.modules.company.infrastructure.mapper.DepartmentTypeMapper;
import com.sme.be_sme.modules.company.infrastructure.persistence.entity.DepartmentEntity;
import com.sme.be_sme.shared.constant.ErrorCodes;
import com.sme.be_sme.shared.exception.AppException;
import com.sme.be_sme.shared.gateway.core.BaseCoreProcessor;
import com.sme.be_sme.shared.util.UuidGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
@RequiredArgsConstructor
public class CreateDepartmentCoreProcessor extends BaseCoreProcessor<CreateDepartmentContext> {

    private final DepartmentMapper departmentMapper;
    private final DepartmentTypeMapper departmentTypeMapper;

    private static final String DEFAULT_STATUS = "ACTIVE";
    private static final String DEFAULT_TYPE = "OTHER";

    @Override
    protected Object process(CreateDepartmentContext ctx) {
        final String tenantId = requireText(ctx.getBiz().getTenantId(), ErrorCodes.UNAUTHORIZED, "missing tenantId");

        final var req = requireNotNull(ctx.getRequest(), ErrorCodes.INVALID_REQUEST, "request is required");
        final String name = requireText(req.getName(), ErrorCodes.INVALID_REQUEST, "name is required").trim();
        final String type = resolveType(tenantId, req.getType());

        if (departmentMapper.countByCompanyAndName(tenantId, name) > 0) {
            throw AppException.of(ErrorCodes.DUPLICATED, "department name already exists");
        }

        final String departmentId = UuidGenerator.generate();
        ctx.setDepartmentId(departmentId);

        DepartmentEntity e = new DepartmentEntity();
        e.setDepartmentId(departmentId);
        e.setCompanyId(tenantId);
        e.setName(name);
        e.setType(type);
        e.setStatus(DEFAULT_STATUS);
        e.setManagerUserId(req.getManagerId());

        if (departmentMapper.insert(e) != 1) {
            throw AppException.of(ErrorCodes.INTERNAL_ERROR, "create department failed");
        }
        return null;
    }

    private static String normalizeOrDefault(String v, String def) {
        return (v == null || v.isBlank()) ? def : v.trim();
    }

    private String resolveType(String tenantId, String requestedType) {
        String type = normalizeOrDefault(requestedType, DEFAULT_TYPE).toUpperCase();
        int configuredTypeCount = departmentTypeMapper.countByCompany(tenantId);
        // Backward compatibility: allow legacy tenants before they configure type master.
        if (configuredTypeCount == 0) {
            return type;
        }
        if (departmentTypeMapper.countByCompanyAndCode(tenantId, type) == 0) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "invalid department type");
        }
        return type;
    }

    private static String requireText(String v, String code, String msg) {
        if (v == null || v.isBlank()) throw AppException.of(code, msg);
        return v;
    }

    private static <T> T requireNotNull(T v, String code, String msg) {
        if (v == null) throw AppException.of(code, msg);
        return v;
    }

}

