package com.sme.be_sme.modules.analytics.infrastructure.mapper;

import com.sme.be_sme.modules.analytics.infrastructure.persistence.entity.CandidateFitAuditEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CandidateFitAuditMapper {
    int insert(CandidateFitAuditEntity row);
}

