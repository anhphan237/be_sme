package com.sme.be_sme.modules.onboarding.infrastructure.mapper;

import com.sme.be_sme.modules.onboarding.infrastructure.persistence.entity.TaskTemplateRequiredDocumentEntity;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface TaskTemplateRequiredDocumentMapper {

    int insert(TaskTemplateRequiredDocumentEntity row);

    int deleteByCompanyIdAndTaskTemplateId(
            @Param("companyId") String companyId,
            @Param("taskTemplateId") String taskTemplateId);

    List<TaskTemplateRequiredDocumentEntity> selectByCompanyIdAndTaskTemplateIds(
            @Param("companyId") String companyId,
            @Param("taskTemplateIds") List<String> taskTemplateIds);
}
