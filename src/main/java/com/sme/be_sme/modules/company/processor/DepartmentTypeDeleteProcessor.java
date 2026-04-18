package com.sme.be_sme.modules.company.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.company.api.request.DeleteDepartmentTypeRequest;
import com.sme.be_sme.modules.company.api.response.DeleteDepartmentTypeResponse;
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
public class DepartmentTypeDeleteProcessor extends BaseBizProcessor<BizContext> {

    private final ObjectMapper objectMapper;
    private final DepartmentTypeMapper departmentTypeMapper;

    @Override
    protected Object doProcess(BizContext context, JsonNode payload) {
        DeleteDepartmentTypeRequest request = objectMapper.convertValue(payload, DeleteDepartmentTypeRequest.class);
        validate(context, request);

        String companyId = context.getTenantId().trim();
        String departmentTypeId = request.getDepartmentTypeId().trim();

        DepartmentTypeEntity existing = departmentTypeMapper.selectByPrimaryKey(departmentTypeId);
        if (existing == null) {
            throw AppException.of(ErrorCodes.NOT_FOUND, "department type not found");
        }
        if (!companyId.equals(existing.getCompanyId())) {
            throw AppException.of(ErrorCodes.FORBIDDEN, "department type not in tenant");
        }

        DepartmentTypeEntity toUpdate = new DepartmentTypeEntity();
        toUpdate.setDepartmentTypeId(departmentTypeId);
        toUpdate.setStatus("INACTIVE");
        if (departmentTypeMapper.updateByPrimaryKeySelective(toUpdate) != 1) {
            throw AppException.of(ErrorCodes.INTERNAL_ERROR, "delete department type failed");
        }

        DeleteDepartmentTypeResponse response = new DeleteDepartmentTypeResponse();
        response.setDepartmentTypeId(departmentTypeId);
        response.setStatus("INACTIVE");
        return response;
    }

    private static void validate(BizContext context, DeleteDepartmentTypeRequest request) {
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
    }
}
