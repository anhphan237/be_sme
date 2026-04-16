package com.sme.be_sme.modules.onboarding.infrastructure.mapper;

import com.sme.be_sme.modules.onboarding.infrastructure.persistence.entity.TaskRequiredDocumentEntity;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface TaskRequiredDocumentMapper {

    int insert(TaskRequiredDocumentEntity row);

    int deleteByCompanyIdAndTaskId(
            @Param("companyId") String companyId,
            @Param("taskId") String taskId);

    List<TaskRequiredDocumentEntity> selectByCompanyIdAndTaskId(
            @Param("companyId") String companyId,
            @Param("taskId") String taskId);
}
