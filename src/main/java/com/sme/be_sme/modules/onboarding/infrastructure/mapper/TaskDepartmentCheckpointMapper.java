package com.sme.be_sme.modules.onboarding.infrastructure.mapper;

import com.sme.be_sme.modules.onboarding.infrastructure.persistence.entity.TaskDepartmentCheckpointEntity;
import com.sme.be_sme.modules.onboarding.infrastructure.persistence.entity.TaskDepartmentDependentListRow;
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

    java.util.List<TaskDepartmentDependentListRow> selectDependentTasksByDepartment(
            @Param("companyId") String companyId,
            @Param("departmentId") String departmentId,
            @Param("checkpointStatus") String checkpointStatus,
            @Param("offset") int offset,
            @Param("limit") int limit
    );

    Integer countDependentTasksByDepartment(
            @Param("companyId") String companyId,
            @Param("departmentId") String departmentId,
            @Param("checkpointStatus") String checkpointStatus
    );
}
