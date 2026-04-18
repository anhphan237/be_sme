package com.sme.be_sme.modules.company.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.company.api.request.UpdateDepartmentTypeRequest;
import com.sme.be_sme.modules.company.api.response.UpdateDepartmentTypeResponse;
import com.sme.be_sme.modules.company.infrastructure.mapper.DepartmentTypeMapper;
import com.sme.be_sme.modules.company.infrastructure.persistence.entity.DepartmentTypeEntity;
import com.sme.be_sme.modules.company.support.CompanyRoleAuth;
import com.sme.be_sme.shared.constant.ErrorCodes;
import com.sme.be_sme.shared.exception.AppException;
import com.sme.be_sme.shared.gateway.core.BaseBizProcessor;
import com.sme.be_sme.shared.gateway.core.BizContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class DepartmentTypeUpdateProcessor extends BaseBizProcessor<BizContext> {

    private final ObjectMapper objectMapper;
    private final DepartmentTypeMapper departmentTypeMapper;

    @Override
    protected Object doProcess(BizContext context, JsonNode payload) {
        UpdateDepartmentTypeRequest request = objectMapper.convertValue(payload, UpdateDepartmentTypeRequest.class);
        validate(context, request);

        String companyId = context.getTenantId().trim();
        String departmentTypeId = request.getDepartmentTypeId().trim();
        String code = request.getCode().trim().toUpperCase();
        String name = request.getName().trim();
        String status = normalizeStatus(request.getStatus());

        DepartmentTypeEntity existing = departmentTypeMapper.selectByPrimaryKey(departmentTypeId);
        if (existing == null) {
            throw AppException.of(ErrorCodes.NOT_FOUND, "department type not found");
        }
        if (!companyId.equals(existing.getCompanyId())) {
            throw AppException.of(ErrorCodes.FORBIDDEN, "department type not in tenant");
        }

        if (departmentTypeMapper.countByCompanyAndCodeExcludeId(companyId, code, departmentTypeId) > 0) {
            throw AppException.of(ErrorCodes.DUPLICATED, "department type code already exists");
        }
        if (departmentTypeMapper.countByCompanyAndNameExcludeId(companyId, name, departmentTypeId) > 0) {
            throw AppException.of(ErrorCodes.DUPLICATED, "department type name already exists");
        }

        DepartmentTypeEntity toUpdate = new DepartmentTypeEntity();
        toUpdate.setDepartmentTypeId(departmentTypeId);
        toUpdate.setCode(code);
        toUpdate.setName(name);
        toUpdate.setStatus(status);
        if (departmentTypeMapper.updateByPrimaryKeySelective(toUpdate) != 1) {
            throw AppException.of(ErrorCodes.INTERNAL_ERROR, "update department type failed");
        }

        UpdateDepartmentTypeResponse response = new UpdateDepartmentTypeResponse();
        response.setDepartmentTypeId(departmentTypeId);
        response.setCompanyId(companyId);
        response.setCode(code);
        response.setName(name);
        response.setStatus(status);
        return response;
    }

    private static void validate(BizContext context, UpdateDepartmentTypeRequest request) {
        if (context == null || !StringUtils.hasText(context.getTenantId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "tenantId is required");
        }
        if (!CompanyRoleAuth.isHrOnly(context.getRoles())) {
            throw AppException.of(ErrorCodes.FORBIDDEN, "only HR can manage department types");
        }
        if (request == null) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "payload is required");
        }
        if (!StringUtils.hasText(request.getDepartmentTypeId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "departmentTypeId is required");
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
            return "ACTIVE";
        }
        String normalized = status.trim().toUpperCase();
        if (!"ACTIVE".equals(normalized) && !"INACTIVE".equals(normalized)) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "invalid status value");
        }
        return normalized;
    }
}
