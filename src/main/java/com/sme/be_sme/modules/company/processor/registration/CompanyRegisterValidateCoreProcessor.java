package com.sme.be_sme.modules.company.processor.registration;

import com.sme.be_sme.modules.company.api.request.CompanyRegisterRequest;
import com.sme.be_sme.modules.company.context.CompanyRegisterContext;
import com.sme.be_sme.shared.constant.ErrorCodes;
import com.sme.be_sme.shared.exception.AppException;
import com.sme.be_sme.shared.gateway.core.BaseCoreProcessor;
import org.springframework.stereotype.Component;

@Component
public class CompanyRegisterValidateCoreProcessor extends BaseCoreProcessor<CompanyRegisterContext> {
    @Override
    protected Object process(CompanyRegisterContext ctx) {
        CompanyRegisterRequest req = ctx.getRequest();
        if (req == null) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "payload is required");
        }
        if (req.getCompany() == null) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "company is required");
        }
        if (req.getAdmin() == null) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "admin is required");
        }
        if (req.getPlanCode() == null || req.getPlanCode().isBlank()) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "planCode is required");
        }

        CompanyRegisterRequest.CompanyInfo company = req.getCompany();
        company.setName(normalize(company.getName()));
        company.setTaxCode(normalize(company.getTaxCode()));
        company.setAddress(normalize(company.getAddress()));
        company.setTimezone(normalize(company.getTimezone()));

        if (isBlank(company.getName())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "company.name is required");
        }
        if (isBlank(company.getTaxCode())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "company.taxCode is required");
        }

        CompanyRegisterRequest.AdminInfo admin = req.getAdmin();
        admin.setUsername(normalizeEmail(admin.getUsername()));
        admin.setFullName(normalize(admin.getFullName()));
        admin.setPhone(normalize(admin.getPhone()));

        if (isBlank(admin.getUsername())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "admin.username is required");
        }
        if (isBlank(admin.getPassword())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "admin.password is required");
        }
        if (isBlank(admin.getFullName())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "admin.fullName is required");
        }
        if (isBlank(admin.getPhone())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "admin.phone is required");
        }
        return null;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String normalizeEmail(String value) {
        String normalized = normalize(value);
        return normalized == null ? null : normalized.toLowerCase();
    }
}
