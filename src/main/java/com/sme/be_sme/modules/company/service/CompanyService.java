package com.sme.be_sme.modules.company.service;

import com.sme.be_sme.modules.company.handler.CompanyHandler;
import com.sme.be_sme.modules.company.infrastructure.persistence.entity.CompanyEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CompanyService {

    private final CompanyHandler companyHandler;

    public void createCompany(CompanyEntity entity) {
        companyHandler.create(entity);
    }
}
