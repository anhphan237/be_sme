package com.sme.be_sme.modules.onboarding.processor;

import com.sme.be_sme.modules.employee.api.request.UpsertEmployeeProfileRequest;
import com.sme.be_sme.modules.employee.processor.UpsertEmployeeProfileProcessor;
import com.sme.be_sme.modules.company.context.CompanySetupContext;
import com.sme.be_sme.shared.constant.ErrorCodes;
import com.sme.be_sme.shared.exception.AppException;
import com.sme.be_sme.shared.gateway.core.BaseCoreProcessor;
import com.sme.be_sme.shared.gateway.core.BizContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@RequiredArgsConstructor
public class CompanySetupUpsertAdminProfileCoreProcessor extends BaseCoreProcessor<CompanySetupContext> {

    private final UpsertEmployeeProfileProcessor upsertEmployeeProfileProcessor;

    @Override
    protected Object process(CompanySetupContext ctx) {
        if (ctx.getCompany() == null || ctx.getCompany().getCompanyId() == null || ctx.getCompany().getCompanyId().isBlank()) {
            throw AppException.of(ErrorCodes.INTERNAL_ERROR, "companyId missing after createCompany");
        }
        if (ctx.getDepartment() == null || ctx.getDepartment().getDepartmentId() == null || ctx.getDepartment().getDepartmentId().isBlank()) {
            throw AppException.of(ErrorCodes.INTERNAL_ERROR, "departmentId missing after createDepartment");
        }
        if (ctx.getAdminUser() == null || ctx.getAdminUser().getUserId() == null || ctx.getAdminUser().getUserId().isBlank()) {
            throw AppException.of(ErrorCodes.INTERNAL_ERROR, "adminUserId missing after createUser");
        }

        String companyId = ctx.getCompany().getCompanyId();
        String requestId = (ctx.getBiz() != null) ? ctx.getBiz().getRequestId() : null;
        BizContext tenantCtx = BizContext.internal(companyId,
                requestId,
                ctx.getAdminUser().getUserId(),
                Set.of("ADMIN")
        );

        // build upsert employee profile request
        UpsertEmployeeProfileRequest upsertReq = UpsertEmployeeProfileRequest.builder()
                .userId(ctx.getAdminUser().getUserId())
                .departmentId(ctx.getDepartment().getDepartmentId())
                .employeeName(ctx.getRequest().getAdminUser().getFullName())
                .employeeEmail(ctx.getRequest().getAdminUser().getEmail())
                .employeePhone(ctx.getRequest().getAdminUser().getPhone())
                .status("ACTIVE")
                .build();

        upsertEmployeeProfileProcessor.process(tenantCtx, upsertReq);
        return null;
    }
}

