package com.sme.be_sme.modules.platform.infrastructure.mapper;

import com.sme.be_sme.modules.platform.api.response.PlatformTemplateDetailChecklistResponse;
import com.sme.be_sme.modules.platform.api.response.PlatformTemplateDetailResponse;
import com.sme.be_sme.modules.platform.api.response.PlatformTemplateDetailTaskResponse;
import com.sme.be_sme.modules.platform.api.response.PlatformTemplateListItemResponse;
import com.sme.be_sme.modules.platform.api.response.PlatformTemplateRequiredDocumentResponse;
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

    PlatformTemplateDetailResponse selectPlatformTemplateDetail(
            @Param("platformCompanyId") String platformCompanyId,
            @Param("templateId") String templateId
    );

    List<PlatformTemplateDetailChecklistResponse> selectTemplateChecklists(
            @Param("platformCompanyId") String platformCompanyId,
            @Param("templateId") String templateId
    );

    List<PlatformTemplateDetailTaskResponse> selectTemplateTasks(
            @Param("platformCompanyId") String platformCompanyId,
            @Param("templateId") String templateId
    );

    List<PlatformTemplateRequiredDocumentResponse> selectRequiredDocumentsByTemplateId(
            @Param("platformCompanyId") String platformCompanyId,
            @Param("templateId") String templateId
    );

    int countTemplateUsage(@Param("templateId") String templateId);

    int deleteRequiredDocumentsByTemplateId(
            @Param("platformCompanyId") String platformCompanyId,
            @Param("templateId") String templateId
    );

    int deleteTasksByTemplateId(
            @Param("platformCompanyId") String platformCompanyId,
            @Param("templateId") String templateId
    );

    int deleteChecklistsByTemplateId(
            @Param("platformCompanyId") String platformCompanyId,
            @Param("templateId") String templateId
    );

    int deleteTemplateById(
            @Param("platformCompanyId") String platformCompanyId,
            @Param("templateId") String templateId
    );
}