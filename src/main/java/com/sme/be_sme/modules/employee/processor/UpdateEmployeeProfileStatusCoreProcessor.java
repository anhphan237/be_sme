package com.sme.be_sme.modules.employee.processor;

import com.sme.be_sme.modules.employee.infrastructure.mapper.EmployeeProfileMapperExt;
import com.sme.be_sme.modules.identity.context.IdentityUserDisableContext;
import com.sme.be_sme.shared.constant.ErrorCodes;
import com.sme.be_sme.shared.exception.AppException;
import com.sme.be_sme.shared.gateway.core.BaseCoreProcessor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
@RequiredArgsConstructor
public class UpdateEmployeeProfileStatusCoreProcessor extends BaseCoreProcessor<IdentityUserDisableContext> {

    private final EmployeeProfileMapperExt employeeProfileMapperExt;

    @Override
    protected Object process(IdentityUserDisableContext ctx) {
        int updated = employeeProfileMapperExt.updateStatusByCompanyIdAndUserId(
                ctx.getBiz().getTenantId(),
                ctx.getRequest().getUserId(),
                ctx.getStatus(),
                new Date()
        );

        if (updated == 0) {
            throw AppException.of(ErrorCodes.NOT_FOUND, "employee profile not found");
        }
        return null;
    }
}

