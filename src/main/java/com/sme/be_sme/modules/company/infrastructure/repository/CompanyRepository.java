package com.sme.be_sme.modules.company.infrastructure.repository;

import com.sme.be_sme.modules.company.infrastructure.persistence.entity.CompanyEntity;

public interface CompanyRepository {
    void insert(CompanyEntity entity);
}
