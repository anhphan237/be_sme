package com.sme.be_sme.modules.onboarding.infrastructure.mapper;

import com.sme.be_sme.modules.onboarding.infrastructure.persistence.entity.TaskDepartmentCheckpointEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TaskDepartmentCheckpointMapper {
    int insert(TaskDepartmentCheckpointEntity row);

    int updateByPrimaryKey(TaskDepartmentCheckpointEntity row);

    List<TaskDepartmentCheckpointEntity> selectByCompanyIdAndTaskId(
            @Param("companyId") String companyId,
            @Param("taskId") String taskId
    );

    TaskDepartmentCheckpointEntity selectByCompanyIdAndTaskIdAndDepartmentId(
            @Param("companyId") String companyId,
            @Param("taskId") String taskId,
            @Param("departmentId") String departmentId
    );

    int countPendingByCompanyIdAndTaskId(
            @Param("companyId") String companyId,
            @Param("taskId") String taskId
    );
}
