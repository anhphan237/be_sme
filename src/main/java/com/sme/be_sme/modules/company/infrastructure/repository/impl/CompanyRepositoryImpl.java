package com.sme.be_sme.modules.company.infrastructure.repository.impl;

import com.sme.be_sme.modules.company.infrastructure.mapper.CompanyMapper;
import com.sme.be_sme.modules.company.infrastructure.persistence.entity.CompanyEntity;
import com.sme.be_sme.modules.company.infrastructure.repository.CompanyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class CompanyRepositoryImpl implements CompanyRepository {

    private final CompanyMapper companyMapper;

    @Override
    public void insert(CompanyEntity entity) {
        companyMapper.insert(entity);
    }
}
