package com.sme.be_sme.modules.company.processor;

import com.sme.be_sme.modules.company.context.UpdateDepartmentContext;
import com.sme.be_sme.modules.company.infrastructure.mapper.DepartmentMapperExt;
import com.sme.be_sme.shared.constant.ErrorCodes;
import com.sme.be_sme.shared.exception.AppException;
import com.sme.be_sme.shared.gateway.core.BaseCoreProcessor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
@RequiredArgsConstructor
public class UpdateDepartmentCoreProcessor extends BaseCoreProcessor<UpdateDepartmentContext> {

    private final DepartmentMapperExt departmentMapperExt;

    @Override
    protected Object process(UpdateDepartmentContext ctx) {
        int updated = departmentMapperExt.updateDepartmentByIdAndCompanyId(
                ctx.getRequest().getDepartmentId(),
                ctx.getBiz().getTenantId(),
                blankToNull(ctx.getRequest().getName()),
                blankToNull(ctx.getRequest().getType()),
                blankToNull(ctx.getRequest().getStatus()),
                new Date()
        );

        if (updated == 0) {
            throw AppException.of(ErrorCodes.NOT_FOUND, "department not found");
        }
        return null;
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }
}
