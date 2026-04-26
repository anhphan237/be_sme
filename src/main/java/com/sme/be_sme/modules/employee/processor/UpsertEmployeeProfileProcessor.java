package com.sme.be_sme.modules.employee.processor;

import com.sme.be_sme.modules.employee.api.request.UpsertEmployeeProfileRequest;
import com.sme.be_sme.modules.employee.api.response.UpsertEmployeeProfileResponse;
import com.sme.be_sme.modules.company.infrastructure.mapper.DepartmentMapper;
import com.sme.be_sme.modules.company.infrastructure.persistence.entity.DepartmentEntity;
import com.sme.be_sme.modules.employee.infrastructure.mapper.EmployeeProfileMapperExt;
import com.sme.be_sme.modules.employee.infrastructure.persistence.entity.EmployeeProfileEntity;
import com.sme.be_sme.modules.employee.service.EmployeeCodeGeneratorService;
import com.sme.be_sme.shared.constant.ErrorCodes;
import com.sme.be_sme.shared.exception.AppException;
import com.sme.be_sme.shared.gateway.core.BizContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Date;

@Component
@RequiredArgsConstructor
public class UpsertEmployeeProfileProcessor {

    private final EmployeeProfileMapperExt employeeProfileMapperExt;
    private final EmployeeCodeGeneratorService employeeCodeGeneratorService;
    private final DepartmentMapper departmentMapper;

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
            String employeeCode = (req.getEmployeeCode() != null && !req.getEmployeeCode().isBlank())
                    ? req.getEmployeeCode().trim()
                    : employeeCodeGeneratorService.generate(companyId);

            EmployeeProfileEntity e = new EmployeeProfileEntity();
            e.setEmployeeId(req.getUserId());
            e.setCompanyId(companyId);
            e.setUserId(req.getUserId());

            e.setDepartmentId(req.getDepartmentId());
            e.setEmployeeCode(employeeCode);
            e.setEmployeeName(req.getEmployeeName());
            e.setEmployeeEmail(req.getEmployeeEmail());
            e.setEmployeePhone(req.getEmployeePhone());

            e.setJobTitle(req.getJobTitle());
            e.setManagerUserId(resolveManagerUserId(companyId, req.getManagerUserId(), req.getDepartmentId(), null));
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
        existing.setManagerUserId(resolveManagerUserId(
                companyId,
                req.getManagerUserId(),
                req.getDepartmentId(),
                existing.getDepartmentId()));
        existing.setStartDate(req.getStartDate());
        existing.setWorkLocation(req.getWorkLocation());

        if (req.getStatus() != null) {
            existing.setStatus(req.getStatus());
        }
        existing.setUpdatedAt(now);

        employeeProfileMapperExt.updateSelectiveByEmployeeId(existing);

        return new UpsertEmployeeProfileResponse(existing.getEmployeeId(), false);
    }

    private String resolveManagerUserId(
            String companyId,
            String requestedManagerUserId,
            String requestedDepartmentId,
            String existingDepartmentId) {
        if (StringUtils.hasText(requestedManagerUserId)) {
            return requestedManagerUserId.trim();
        }
        String departmentId = StringUtils.hasText(requestedDepartmentId)
                ? requestedDepartmentId.trim()
                : (StringUtils.hasText(existingDepartmentId) ? existingDepartmentId.trim() : null);
        if (!StringUtils.hasText(departmentId)) {
            return null;
        }
        DepartmentEntity department = departmentMapper.selectByPrimaryKey(departmentId);
        if (department == null || !companyId.equals(department.getCompanyId())) {
            return null;
        }
        return StringUtils.hasText(department.getManagerUserId()) ? department.getManagerUserId().trim() : null;
    }
}
