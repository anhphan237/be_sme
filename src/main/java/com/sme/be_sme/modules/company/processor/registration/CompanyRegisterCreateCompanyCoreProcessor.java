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

        String companyCode = normalizeCompanyCode(company.getCode(), company.getName(), companyId);

        CompanyEntity entity = new CompanyEntity();
        entity.setCompanyId(companyId);
        entity.setName(company.getName());
        entity.setTaxCode(company.getTaxCode());
        entity.setCode(companyCode);
        entity.setAddress(company.getAddress());
        entity.setTimezone(company.getTimezone() == null ? "Asia/Ho_Chi_Minh" : company.getTimezone());
        String planCode = ctx.getRequest().getPlanCode();
        boolean isFree = planCode == null || planCode.isBlank() || "FREE".equalsIgnoreCase(planCode.trim());
        entity.setStatus(isFree ? "ACTIVE" : "PENDING_PAYMENT");
        entity.setIndustry(company.getIndustry());
        entity.setCompanySize(company.getCompanySize());
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

    /** Derive 3-char company code from code, name, or companyId. */
    private static String normalizeCompanyCode(String code, String name, String companyId) {
        String raw = null;
        if (code != null && !code.isBlank()) {
            raw = code.replaceAll("[^a-zA-Z0-9]", "").toUpperCase();
        }
        if (raw == null || raw.isEmpty()) {
            if (name != null && !name.isBlank()) {
                raw = name.replaceAll("[^a-zA-Z0-9]", "").toUpperCase();
            }
        }
        if (raw == null || raw.isEmpty()) {
            raw = (companyId != null && companyId.length() >= 3)
                    ? companyId.substring(0, 3).toUpperCase().replaceAll("[^A-Z0-9]", "")
                    : null;
        }
        if (raw == null || raw.isEmpty()) {
            raw = "CMP";
        }
        return String.format("%-3s", raw.length() >= 3 ? raw.substring(0, 3) : raw)
                .replace(' ', 'A').substring(0, 3);
    }
}
