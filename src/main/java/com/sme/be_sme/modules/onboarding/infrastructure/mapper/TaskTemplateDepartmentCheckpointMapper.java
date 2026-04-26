package com.sme.be_sme.modules.onboarding.infrastructure.mapper;

import com.sme.be_sme.modules.onboarding.infrastructure.persistence.entity.TaskTemplateDepartmentCheckpointEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TaskTemplateDepartmentCheckpointMapper {
    int insert(TaskTemplateDepartmentCheckpointEntity row);

    int deleteByCompanyIdAndTaskTemplateId(
            @Param("companyId") String companyId,
            @Param("taskTemplateId") String taskTemplateId
    );

    List<TaskTemplateDepartmentCheckpointEntity> selectByCompanyIdAndTaskTemplateIds(
            @Param("companyId") String companyId,
            @Param("taskTemplateIds") List<String> taskTemplateIds
    );
}
