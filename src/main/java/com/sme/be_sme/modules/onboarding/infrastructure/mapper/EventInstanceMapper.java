package com.sme.be_sme.modules.onboarding.infrastructure.mapper;

import com.sme.be_sme.modules.onboarding.infrastructure.persistence.entity.EventInstanceEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface EventInstanceMapper {
    int insert(EventInstanceEntity row);
}
