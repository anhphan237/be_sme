package com.sme.be_sme.modules.company.processor.registration;

import com.sme.be_sme.modules.company.api.request.CompanyRegisterRequest;
import com.sme.be_sme.modules.company.context.CompanyRegisterContext;
import com.sme.be_sme.modules.company.infrastructure.mapper.CompanyMapper;
import com.sme.be_sme.modules.company.infrastructure.persistence.entity.CompanyEntity;
import com.sme.be_sme.shared.constant.ErrorCodes;
import com.sme.be_sme.shared.exception.AppException;
import com.sme.be_sme.shared.gateway.core.BaseCoreProcessor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Date;
import com.sme.be_sme.shared.util.UuidGenerator;

@Component
@RequiredArgsConstructor
public class CompanyRegisterCreateCompanyCoreProcessor extends BaseCoreProcessor<CompanyRegisterContext> {

    private final CompanyMapper companyMapper;

    @Override
    protected Object process(CompanyRegisterContext ctx) {
        CompanyRegisterRequest.CompanyInfo company = ctx.getRequest().getCompany();
        String companyId = UuidGenerator.generate();
        Date now = new Date();

        CompanyEntity entity = new CompanyEntity();
        entity.setCompanyId(companyId);
        entity.setName(company.getName());
        entity.setTaxCode(company.getTaxCode());
        entity.setAddress(company.getAddress());
        entity.setTimezone(company.getTimezone() == null ? "Asia/Ho_Chi_Minh" : company.getTimezone());
        entity.setStatus("ACTIVE");
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);

        int inserted = companyMapper.insert(entity);
        if (inserted != 1) {
            throw AppException.of(ErrorCodes.INTERNAL_ERROR, "create company failed");
        }

        ctx.setCompany(entity);
        ctx.setCompanyId(companyId);
        return null;
    }
}
