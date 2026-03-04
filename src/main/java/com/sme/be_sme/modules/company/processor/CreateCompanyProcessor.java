package com.sme.be_sme.modules.company.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.company.api.request.CreateCompanyRequest;
import com.sme.be_sme.modules.company.api.response.CreateCompanyResponse;
import com.sme.be_sme.modules.company.infrastructure.persistence.entity.CompanyEntity;
import com.sme.be_sme.modules.company.service.CompanyService;
import com.sme.be_sme.shared.constant.ErrorCodes;
import com.sme.be_sme.shared.exception.AppException;
import com.sme.be_sme.shared.gateway.core.BaseBizProcessor;
import com.sme.be_sme.shared.gateway.core.BizContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CreateCompanyProcessor extends BaseBizProcessor<BizContext> {

    private final CompanyService companyService;
    private final ObjectMapper objectMapper;

    @Override
    protected Object doProcess(BizContext context, JsonNode payload) {
        CreateCompanyRequest req = objectMapper.convertValue(payload, CreateCompanyRequest.class);
        return process(context, req);
    }

    public CreateCompanyResponse process(BizContext context, CreateCompanyRequest request) {
        if (request == null || request.getName() == null || request.getName().isBlank()) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "company.name is required");
        }

        String companyId = (request.getCompanyId() != null && !request.getCompanyId().isBlank())
                ? request.getCompanyId()
                : UUID.randomUUID().toString();

        String companyCode = deriveCompanyCode(request.getCode(), request.getName(), companyId);

        Date now = new Date();
        CompanyEntity entity = new CompanyEntity();
        entity.setCompanyId(companyId);
        entity.setName(request.getName());
        entity.setTaxCode(request.getTaxCode());
        entity.setCode(companyCode);
        entity.setAddress(request.getAddress());
        entity.setTimezone(request.getTimezone() == null ? "Asia/Ho_Chi_Minh" : request.getTimezone());
        entity.setStatus(request.getStatus() == null ? "ACTIVE" : request.getStatus());
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);

        companyService.createCompany(entity);

        CreateCompanyResponse res = new CreateCompanyResponse();
        res.setCompanyId(companyId);
        res.setName(entity.getName());
        res.setStatus(entity.getStatus());
        return res;
    }

    private static String deriveCompanyCode(String code, String name, String companyId) {
        String raw = null;
        if (code != null && !code.isBlank()) {
            raw = code.replaceAll("[^a-zA-Z0-9]", "").toUpperCase();
        }
        if ((raw == null || raw.isEmpty()) && name != null && !name.isBlank()) {
            raw = name.replaceAll("[^a-zA-Z0-9]", "").toUpperCase();
        }
        if ((raw == null || raw.isEmpty()) && companyId != null && companyId.length() >= 3) {
            raw = companyId.substring(0, 3).toUpperCase().replaceAll("[^A-Z0-9]", "");
        }
        if (raw == null || raw.isEmpty()) {
            raw = "CMP";
        }
        return String.format("%-3s", raw.length() >= 3 ? raw.substring(0, 3) : raw)
                .replace(' ', 'A').substring(0, 3);
    }

}
