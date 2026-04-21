package com.sme.be_sme.modules.onboarding.infrastructure.mapper;

import com.sme.be_sme.modules.onboarding.infrastructure.persistence.entity.EventTemplateEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface EventTemplateMapper {
    int insert(EventTemplateEntity row);

    EventTemplateEntity selectByCompanyIdAndTemplateId(
            @Param("companyId") String companyId,
            @Param("eventTemplateId") String eventTemplateId);
}
