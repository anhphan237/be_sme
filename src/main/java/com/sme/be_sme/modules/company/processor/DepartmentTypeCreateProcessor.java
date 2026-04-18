package com.sme.be_sme.modules.company.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.company.api.request.CreateDepartmentTypeRequest;
import com.sme.be_sme.modules.company.api.response.CreateDepartmentTypeResponse;
import com.sme.be_sme.modules.company.infrastructure.mapper.DepartmentTypeMapper;
import com.sme.be_sme.modules.company.infrastructure.persistence.entity.DepartmentTypeEntity;
import com.sme.be_sme.modules.company.support.CompanyRoleAuth;
import com.sme.be_sme.shared.constant.ErrorCodes;
import com.sme.be_sme.shared.exception.AppException;
import com.sme.be_sme.shared.gateway.core.BaseBizProcessor;
import com.sme.be_sme.shared.gateway.core.BizContext;
import com.sme.be_sme.shared.util.UuidGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class DepartmentTypeCreateProcessor extends BaseBizProcessor<BizContext> {

    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_INACTIVE = "INACTIVE";

    private final ObjectMapper objectMapper;
    private final DepartmentTypeMapper departmentTypeMapper;

    @Override
    protected Object doProcess(BizContext context, JsonNode payload) {
        CreateDepartmentTypeRequest request = objectMapper.convertValue(payload, CreateDepartmentTypeRequest.class);
        validate(context, request);

        String companyId = context.getTenantId().trim();
        String code = request.getCode().trim().toUpperCase();
        String name = request.getName().trim();
        String status = normalizeStatus(request.getStatus());

        if (departmentTypeMapper.countByCompanyAndCode(companyId, code) > 0) {
            throw AppException.of(ErrorCodes.DUPLICATED, "department type code already exists");
        }
        if (departmentTypeMapper.countByCompanyAndName(companyId, name) > 0) {
            throw AppException.of(ErrorCodes.DUPLICATED, "department type name already exists");
        }

        DepartmentTypeEntity entity = new DepartmentTypeEntity();
        entity.setDepartmentTypeId(UuidGenerator.generate());
        entity.setCompanyId(companyId);
        entity.setCode(code);
        entity.setName(name);
        entity.setStatus(status);
        if (departmentTypeMapper.insert(entity) != 1) {
            throw AppException.of(ErrorCodes.INTERNAL_ERROR, "create department type failed");
        }

        CreateDepartmentTypeResponse response = new CreateDepartmentTypeResponse();
        response.setDepartmentTypeId(entity.getDepartmentTypeId());
        response.setCompanyId(companyId);
        response.setCode(code);
        response.setName(name);
        response.setStatus(status);
        return response;
    }

    private static void validate(BizContext context, CreateDepartmentTypeRequest request) {
        if (context == null || !StringUtils.hasText(context.getTenantId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "tenantId is required");
        }
        if (!CompanyRoleAuth.isHrOnly(context.getRoles())) {
            throw AppException.of(ErrorCodes.FORBIDDEN, "only HR can manage department types");
        }
        if (request == null) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "payload is required");
        }
        if (!StringUtils.hasText(request.getCode())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "code is required");
        }
        if (!StringUtils.hasText(request.getName())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "name is required");
        }
    }

    private static String normalizeStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return STATUS_ACTIVE;
        }
        String normalized = status.trim().toUpperCase();
        if (!STATUS_ACTIVE.equals(normalized) && !STATUS_INACTIVE.equals(normalized)) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "invalid status value");
        }
        return normalized;
    }
}
