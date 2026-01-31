package com.sme.be_sme.modules.company.processor.registration;

import com.sme.be_sme.modules.company.api.request.CompanyRegisterRequest;
import com.sme.be_sme.modules.company.context.CompanyRegisterContext;
import com.sme.be_sme.modules.company.infrastructure.mapper.CompanyMapper;
import com.sme.be_sme.modules.identity.infrastructure.mapper.UserMapperExt;
import com.sme.be_sme.shared.exception.AppException;
import com.sme.be_sme.shared.gateway.core.BaseCoreProcessor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CompanyRegisterCheckDupCoreProcessor extends BaseCoreProcessor<CompanyRegisterContext> {

    private final CompanyMapper companyMapper;
    private final UserMapperExt userMapperExt;

    @Override
    protected Object process(CompanyRegisterContext ctx) {
        CompanyRegisterRequest request = ctx.getRequest();
        CompanyRegisterRequest.CompanyInfo company = request.getCompany();
        int companyCount = companyMapper.countByTaxCodeOrName(company.getTaxCode(), company.getName());
        if (companyCount > 0) {
            throw AppException.of("COMPANY_ALREADY_EXISTS", "company already exists");
        }

        String email = request.getAdmin().getUsername();
        int adminCount = userMapperExt.countByEmail(email);
        if (adminCount > 0) {
            throw AppException.of("ADMIN_ALREADY_EXISTS", "admin already exists");
        }
        return null;
    }
}
