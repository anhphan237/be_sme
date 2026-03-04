package com.sme.be_sme.modules.onboarding.infrastructure.mapper;

import com.sme.be_sme.modules.onboarding.infrastructure.persistence.entity.TaskCommentEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TaskCommentMapperExt {

    /**
     * Select all comments for a specific task
     *
     * @param companyId Tenant ID
     * @param taskId Task ID
     * @return List of comments ordered by created_at DESC
     */
    List<TaskCommentEntity> selectByTaskId(
        @Param("companyId") String companyId,
        @Param("taskId") String taskId
    );
}
