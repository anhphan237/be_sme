package com.sme.be_sme.modules.company.processor;

import com.sme.be_sme.modules.company.context.CreateDepartmentContext;
import com.sme.be_sme.modules.company.infrastructure.mapper.DepartmentMapper;
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

    @Override
    protected Object process(CreateDepartmentContext ctx) {
        String departmentId = UuidGenerator.generate();
        ctx.setDepartmentId(departmentId);

        Date now = new Date();

        DepartmentEntity e = new DepartmentEntity();
        e.setDepartmentId(departmentId);
        e.setCompanyId(ctx.getBiz().getTenantId());
        e.setName(ctx.getRequest().getName());
        e.setType(ctx.getRequest().getType());
        e.setStatus(ctx.getRequest().getStatus() != null ? ctx.getRequest().getStatus() : "ACTIVE");
        e.setCreatedAt(now);
        e.setUpdatedAt(now);

        int inserted = departmentMapper.insert(e);
        if (inserted != 1) {
            throw AppException.of(ErrorCodes.INTERNAL_ERROR, "create department failed");
        }
        return null;
    }
}

