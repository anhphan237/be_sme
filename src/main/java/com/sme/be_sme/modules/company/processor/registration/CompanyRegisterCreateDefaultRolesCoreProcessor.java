package com.sme.be_sme.modules.company.processor.registration;

import com.sme.be_sme.modules.company.context.CompanyRegisterContext;
import com.sme.be_sme.modules.identity.infrastructure.mapper.RoleMapper;
import com.sme.be_sme.modules.identity.infrastructure.persistence.entity.RoleEntity;
import com.sme.be_sme.shared.constant.ErrorCodes;
import com.sme.be_sme.shared.exception.AppException;
import com.sme.be_sme.shared.gateway.core.BaseCoreProcessor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import com.sme.be_sme.shared.util.UuidGenerator;

@Component
@RequiredArgsConstructor
public class CompanyRegisterCreateDefaultRolesCoreProcessor extends BaseCoreProcessor<CompanyRegisterContext> {

    private final RoleMapper roleMapper;

    @Override
    protected Object process(CompanyRegisterContext ctx) {
        if (ctx.getCompany() == null || ctx.getCompany().getCompanyId() == null) {
            throw AppException.of(ErrorCodes.INTERNAL_ERROR, "companyId missing after createCompany");
        }

        String companyId = ctx.getCompany().getCompanyId();
        Date now = new Date();
        List<RoleEntity> roles = new ArrayList<>();

        roles.add(buildRole(companyId, "ADMIN", "ADMIN", now));
        roles.add(buildRole(companyId, "HR", "HR", now));
        roles.add(buildRole(companyId, "MANAGER", "MANAGER", now));
        roles.add(buildRole(companyId, "IT", "IT", now));
        roles.add(buildRole(companyId, "EMPLOYEE", "EMPLOYEE", now));

        roleMapper.insertBatch(roles);
        ctx.setDefaultRoles(roles);
        return null;
    }

    private RoleEntity buildRole(String companyId, String code, String name, Date now) {
        RoleEntity role = new RoleEntity();
        role.setRoleId(UuidGenerator.generate());
        role.setCompanyId(companyId);
        role.setCode(code);
        role.setName(name);
        role.setStatus("ACTIVE");
        role.setCreatedAt(now);
        role.setUpdatedAt(now);
        return role;
    }
}
