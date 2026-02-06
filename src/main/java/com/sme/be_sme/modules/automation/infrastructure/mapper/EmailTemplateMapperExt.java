package com.sme.be_sme.modules.automation.infrastructure.mapper;

import com.sme.be_sme.modules.automation.infrastructure.persistence.entity.EmailTemplateEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface EmailTemplateMapperExt {

    /**
     * Resolve template by company and name (template code).
     * Tries (companyId, name) first, then (NULL, name) for system templates.
     */
    EmailTemplateEntity selectByCompanyIdAndName(
            @Param("companyId") String companyId,
            @Param("name") String name
    );
}
