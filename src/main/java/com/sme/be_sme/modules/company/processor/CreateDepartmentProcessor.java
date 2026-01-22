package com.sme.be_sme.modules.company.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.company.api.request.CreateDepartmentRequest;
import com.sme.be_sme.modules.company.api.response.CreateDepartmentResponse;
import com.sme.be_sme.modules.company.infrastructure.persistence.entity.DepartmentEntity;
import com.sme.be_sme.modules.company.service.DepartmentService;
import com.sme.be_sme.shared.constant.ErrorCodes;
import com.sme.be_sme.shared.exception.AppException;
import com.sme.be_sme.shared.gateway.core.BaseBizProcessor;
import com.sme.be_sme.shared.gateway.core.BizContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CreateDepartmentProcessor extends BaseBizProcessor<BizContext> {

    private final DepartmentService departmentService;
    private final ObjectMapper objectMapper;

    @Override
    protected Object doProcess(BizContext context, JsonNode payload) {
        CreateDepartmentRequest req = objectMapper.convertValue(payload, CreateDepartmentRequest.class);
        return process(context, req);
    }

    public CreateDepartmentResponse process(BizContext context, CreateDepartmentRequest request) {
        if (request == null || request.getName() == null || request.getName().isBlank()) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "department.name is required");
        }

        BizContext safeContext = context == null ? BizContext.of(null, null) : context;
        String companyId = resolveCompanyId(safeContext.getTenantId(), request);
        String departmentId = (request.getDepartmentId() != null && !request.getDepartmentId().isBlank())
                ? request.getDepartmentId()
                : UUID.randomUUID().toString();

        Date now = new Date();
        DepartmentEntity entity = new DepartmentEntity();
        entity.setDepartmentId(departmentId);
        entity.setCompanyId(companyId);
        entity.setName(request.getName());
        entity.setType(request.getType() == null ? "DEFAULT" : request.getType());
        entity.setStatus(request.getStatus() == null ? "ACTIVE" : request.getStatus());
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);

        departmentService.createDepartment(entity);

        CreateDepartmentResponse res = new CreateDepartmentResponse();
        res.setDepartmentId(departmentId);
        res.setCompanyId(companyId);
        res.setName(entity.getName());
        res.setStatus(entity.getStatus());
        return res;
    }

    public CreateDepartmentResponse process(String tenantId, CreateDepartmentRequest request) {
        return process(BizContext.of(tenantId, null), request);
    }

    private String resolveCompanyId(String tenantId, CreateDepartmentRequest request) {
        if (tenantId != null && !tenantId.isBlank()) {
            return tenantId;
        }
        if (request.getCompanyId() != null && !request.getCompanyId().isBlank()) {
            return request.getCompanyId();
        }
        throw AppException.of(ErrorCodes.BAD_REQUEST, "companyId is required");
    }
}
