package com.sme.be_sme.modules.onboarding.infrastructure.mapper;

import com.sme.be_sme.modules.onboarding.infrastructure.persistence.entity.TaskAttachmentEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TaskAttachmentMapperExt {

    /**
     * Select all attachments for a specific task
     *
     * @param companyId Tenant ID
     * @param taskId Task ID
     * @return List of attachments ordered by uploaded_at DESC
     */
    List<TaskAttachmentEntity> selectByTaskId(
        @Param("companyId") String companyId,
        @Param("taskId") String taskId
    );
}
