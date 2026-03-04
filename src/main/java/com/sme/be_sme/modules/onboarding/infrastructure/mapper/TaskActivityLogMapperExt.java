package com.sme.be_sme.modules.onboarding.infrastructure.mapper;

import com.sme.be_sme.modules.onboarding.infrastructure.persistence.entity.TaskActivityLogEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TaskActivityLogMapperExt {

    /**
     * Select all activity logs for a specific task
     *
     * @param companyId Tenant ID
     * @param taskId Task ID
     * @return List of activity logs ordered by created_at DESC
     */
    List<TaskActivityLogEntity> selectByTaskId(
        @Param("companyId") String companyId,
        @Param("taskId") String taskId
    );
}
