package com.sme.be_sme.modules.employee.service;

import com.sme.be_sme.modules.company.infrastructure.mapper.CompanyMapper;
import com.sme.be_sme.modules.company.infrastructure.persistence.entity.CompanyEntity;
import com.sme.be_sme.modules.employee.infrastructure.mapper.EmployeeProfileMapperExt;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Generates employee code in format [ABC]000001:
 * - 3 chars: company code
 * - 6 digits: employee sequence within company
 */
@Service
@RequiredArgsConstructor
public class EmployeeCodeGeneratorService {

    private final CompanyMapper companyMapper;
    private final EmployeeProfileMapperExt employeeProfileMapperExt;

    public String generate(String companyId) {
        String companyCode = resolveCompanyCode(companyId);
        int nextSeq = employeeProfileMapperExt.getNextEmployeeSequence(companyId);
        return String.format("[%s]%06d", companyCode, nextSeq);
    }

    private String resolveCompanyCode(String companyId) {
        CompanyEntity company = companyMapper.selectByPrimaryKey(companyId);
        if (company != null && StringUtils.hasText(company.getCode()) && company.getCode().length() >= 3) {
            return company.getCode().substring(0, 3).toUpperCase().replaceAll("[^A-Z0-9]", "A");
        }
        if (company != null && StringUtils.hasText(company.getName())) {
            String fromName = company.getName().replaceAll("[^a-zA-Z0-9]", "").toUpperCase();
            if (fromName.length() >= 3) {
                return fromName.substring(0, 3);
            }
            if (!fromName.isEmpty()) {
                return String.format("%-3s", fromName).replace(' ', 'A').substring(0, 3);
            }
        }
        return (companyId != null && companyId.length() >= 3)
                ? companyId.substring(0, 3).toUpperCase().replaceAll("[^A-Z0-9]", "A")
                : "CMP";
    }
}
