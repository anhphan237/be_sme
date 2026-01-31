package com.sme.be_sme.modules.company.processor.registration;

import com.sme.be_sme.modules.company.api.request.CompanyRegisterRequest;
import com.sme.be_sme.modules.company.context.CompanyRegisterContext;
import com.sme.be_sme.modules.identity.infrastructure.mapper.UserMapper;
import com.sme.be_sme.modules.identity.infrastructure.persistence.entity.UserEntity;
import com.sme.be_sme.shared.constant.ErrorCodes;
import com.sme.be_sme.shared.exception.AppException;
import com.sme.be_sme.shared.gateway.core.BaseCoreProcessor;
import com.sme.be_sme.shared.security.PasswordHasher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Date;
import com.sme.be_sme.shared.util.UuidGenerator;

@Component
@RequiredArgsConstructor
public class CompanyRegisterCreateAdminUserCoreProcessor extends BaseCoreProcessor<CompanyRegisterContext> {

    private final UserMapper userMapper;
    private final PasswordHasher passwordHasher;

    @Override
    protected Object process(CompanyRegisterContext ctx) {
        if (ctx.getCompany() == null || ctx.getCompany().getCompanyId() == null) {
            throw AppException.of(ErrorCodes.INTERNAL_ERROR, "companyId missing after createCompany");
        }

        CompanyRegisterRequest.AdminInfo admin = ctx.getRequest().getAdmin();
        String userId = UuidGenerator.generate();
        Date now = new Date();

        UserEntity entity = new UserEntity();
        entity.setUserId(userId);
        entity.setCompanyId(ctx.getCompany().getCompanyId());
        entity.setEmail(admin.getUsername());
        entity.setPasswordHash(passwordHasher.hash(admin.getPassword()));
        entity.setFullName(admin.getFullName());
        entity.setPhone(admin.getPhone());
        entity.setStatus("ACTIVE");
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);

        int inserted = userMapper.insert(entity);
        if (inserted != 1) {
            throw AppException.of(ErrorCodes.INTERNAL_ERROR, "create admin user failed");
        }

        ctx.setAdminUser(entity);
        ctx.setAdminUserId(userId);
        return null;
    }
}
