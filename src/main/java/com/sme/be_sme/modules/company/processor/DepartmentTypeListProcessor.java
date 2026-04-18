package com.sme.be_sme.modules.company.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.company.api.model.DepartmentTypeItem;
import com.sme.be_sme.modules.company.api.request.ListDepartmentTypeRequest;
import com.sme.be_sme.modules.company.api.response.ListDepartmentTypeResponse;
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

import java.util.List;

@Component
@RequiredArgsConstructor
public class DepartmentTypeListProcessor extends BaseBizProcessor<BizContext> {

    private final ObjectMapper objectMapper;
    private final DepartmentTypeMapper departmentTypeMapper;

    @Override
    protected Object doProcess(BizContext context, JsonNode payload) {
        ListDepartmentTypeRequest request =
                payload == null || payload.isNull() || payload.isEmpty()
                        ? new ListDepartmentTypeRequest()
                        : objectMapper.convertValue(payload, ListDepartmentTypeRequest.class);
        validate(context, request);

        String companyId = context.getTenantId().trim();
        String status = normalizeStatus(request.getStatus());

        List<DepartmentTypeEntity> entities = StringUtils.hasText(status)
                ? departmentTypeMapper.selectByCompanyAndStatus(companyId, status)
                : departmentTypeMapper.selectByCompany(companyId);

        ListDepartmentTypeResponse response = new ListDepartmentTypeResponse();
        response.setItems(entities.stream().map(e -> {
            DepartmentTypeItem item = new DepartmentTypeItem();
            item.setDepartmentTypeId(e.getDepartmentTypeId());
            item.setCode(e.getCode());
            item.setName(e.getName());
            item.setStatus(e.getStatus());
            return item;
        }).toList());
        return response;
    }

    private static void validate(BizContext context, ListDepartmentTypeRequest request) {
        if (context == null || !StringUtils.hasText(context.getTenantId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "tenantId is required");
        }
        if (!CompanyRoleAuth.isHrOnly(context.getRoles())) {
            throw AppException.of(ErrorCodes.FORBIDDEN, "only HR can manage department types");
        }
        if (request == null) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "payload is required");
        }
    }

    private static String normalizeStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return null;
        }
        String normalized = status.trim().toUpperCase();
        if (!"ACTIVE".equals(normalized) && !"INACTIVE".equals(normalized)) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "invalid status value");
        }
        return normalized;
    }
}
