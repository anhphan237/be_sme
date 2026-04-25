package com.sme.be_sme.modules.onboarding.infrastructure.mapper;

import com.sme.be_sme.modules.onboarding.infrastructure.persistence.entity.EventTemplateEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface EventTemplateMapper {
    int insert(EventTemplateEntity row);

    EventTemplateEntity selectByCompanyIdAndTemplateId(
            @Param("companyId") String companyId,
            @Param("eventTemplateId") String eventTemplateId);

    List<EventTemplateEntity> selectByCompanyIdAndTemplateIds(
            @Param("companyId") String companyId,
            @Param("eventTemplateIds") List<String> eventTemplateIds);
}
