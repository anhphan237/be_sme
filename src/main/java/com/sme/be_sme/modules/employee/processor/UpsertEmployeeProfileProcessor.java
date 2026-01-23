package com.sme.be_sme.modules.employee.processor;

import com.sme.be_sme.modules.employee.api.request.UpsertEmployeeProfileRequest;
import com.sme.be_sme.modules.employee.api.response.UpsertEmployeeProfileResponse;
import com.sme.be_sme.modules.employee.infrastructure.persistence.entity.EmployeeProfileEntity;
import com.sme.be_sme.modules.employee.infrastructure.mapper.EmployeeProfileMapperExt;
import com.sme.be_sme.shared.constant.ErrorCodes;
import com.sme.be_sme.shared.exception.AppException;
import com.sme.be_sme.shared.gateway.core.BizContext;
import com.sme.be_sme.shared.util.UuidGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
@RequiredArgsConstructor
public class UpsertEmployeeProfileProcessor {

    private final EmployeeProfileMapperExt employeeProfileMapperExt;

    public UpsertEmployeeProfileResponse process(BizContext ctx, UpsertEmployeeProfileRequest req) {
        if (ctx == null || ctx.getTenantId() == null || ctx.getTenantId().isBlank()) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "tenantId is required");
        }
        if (req == null || req.getUserId() == null || req.getUserId().isBlank()) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "userId is required");
        }

        String companyId = ctx.getTenantId();
        Date now = new Date();

        // 1) find existing by (company_id, user_id)
        EmployeeProfileEntity existing =
                employeeProfileMapperExt.selectByCompanyIdAndUserId(companyId, req.getUserId());

        if (existing == null) {
            EmployeeProfileEntity e = new EmployeeProfileEntity();
            e.setEmployeeId(UuidGenerator.generate());
            e.setCompanyId(companyId);
            e.setUserId(req.getUserId());

            e.setDepartmentId(req.getDepartmentId());
            e.setEmployeeCode(req.getEmployeeCode());
            e.setEmployeeName(req.getEmployeeName());
            e.setEmployeeEmail(req.getEmployeeEmail());
            e.setEmployeePhone(req.getEmployeePhone());

            e.setJobTitle(req.getJobTitle());
            e.setManagerUserId(req.getManagerUserId());
            e.setStartDate(req.getStartDate());
            e.setWorkLocation(req.getWorkLocation());

            e.setStatus(req.getStatus() == null ? "ACTIVE" : req.getStatus());
            e.setCreatedAt(now);
            e.setUpdatedAt(now);

            employeeProfileMapperExt.insert(e);

            return new UpsertEmployeeProfileResponse(e.getEmployeeId(), true);
        }

        // 2) update selective
        existing.setDepartmentId(req.getDepartmentId());
        existing.setEmployeeCode(req.getEmployeeCode());
        existing.setEmployeeName(req.getEmployeeName());
        existing.setEmployeeEmail(req.getEmployeeEmail());
        existing.setEmployeePhone(req.getEmployeePhone());

        existing.setJobTitle(req.getJobTitle());
        existing.setManagerUserId(req.getManagerUserId());
        existing.setStartDate(req.getStartDate());
        existing.setWorkLocation(req.getWorkLocation());

        if (req.getStatus() != null) {
            existing.setStatus(req.getStatus());
        }
        existing.setUpdatedAt(now);

        employeeProfileMapperExt.updateSelectiveByEmployeeId(existing);

        return new UpsertEmployeeProfileResponse(existing.getEmployeeId(), false);
    }
}
