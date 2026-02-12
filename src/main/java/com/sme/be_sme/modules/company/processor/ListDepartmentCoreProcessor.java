package com.sme.be_sme.modules.company.processor;

import com.sme.be_sme.modules.company.api.model.DepartmentItem;
import com.sme.be_sme.modules.company.context.ListDepartmentContext;
import com.sme.be_sme.modules.company.infrastructure.mapper.DepartmentMapper;
import com.sme.be_sme.modules.company.infrastructure.persistence.entity.DepartmentEntity;
import com.sme.be_sme.shared.gateway.core.BaseCoreProcessor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ListDepartmentCoreProcessor
        extends BaseCoreProcessor<ListDepartmentContext> {

    private final DepartmentMapper departmentMapper;

    @Override
    protected Object process(ListDepartmentContext ctx) {
        String tenantId = ctx.getBiz().getTenantId();
        String status = ctx.getRequest().getStatus();

        List<DepartmentEntity> entities =
                (status == null || status.isBlank())
                        ? departmentMapper.selectByCompany(tenantId)
                        : departmentMapper.selectByCompanyAndStatus(tenantId, status);

        List<DepartmentItem> items = entities.stream().map(e -> {
            DepartmentItem i = new DepartmentItem();
            i.setDepartmentId(e.getDepartmentId());
            i.setName(e.getName());
            i.setType(e.getType());
            i.setManagerUserId(e.getManagerUserId());
            return i;
        }).toList();

        ctx.setItems(items);
        return null;
    }
}


