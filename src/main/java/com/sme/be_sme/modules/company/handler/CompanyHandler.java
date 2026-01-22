package com.sme.be_sme.modules.company.handler;

import com.sme.be_sme.modules.company.infrastructure.persistence.entity.CompanyEntity;
import com.sme.be_sme.modules.company.infrastructure.repository.CompanyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CompanyHandler {

    private final CompanyRepository companyRepository;

    public void create(CompanyEntity entity) {
        companyRepository.insert(entity);
    }
}
