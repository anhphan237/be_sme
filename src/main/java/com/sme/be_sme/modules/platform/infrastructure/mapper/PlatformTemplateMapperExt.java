package com.sme.be_sme.modules.platform.infrastructure.mapper;


import com.sme.be_sme.modules.platform.api.response.PlatformTemplateListItemResponse;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PlatformTemplateMapperExt {

    List<PlatformTemplateListItemResponse> selectPlatformTemplates(
            @Param("platformCompanyId") String platformCompanyId,
            @Param("keyword") String keyword,
            @Param("status") String status,
            @Param("limit") int limit,
            @Param("offset") int offset
    );

    int countPlatformTemplates(
            @Param("platformCompanyId") String platformCompanyId,
            @Param("keyword") String keyword,
            @Param("status") String status
    );
}