package com.sme.be_sme.modules.onboarding.infrastructure.mapper;

import com.sme.be_sme.modules.onboarding.infrastructure.persistence.entity.TaskInstanceEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

@Mapper
public interface TaskInstanceMapperExt {

    /**
     * Tasks with due_date between fromDate and toDate (inclusive) and status != DONE (for reminder job).
     */
    List<TaskInstanceEntity> selectDueBetweenAndStatusNotDone(
            @Param("fromDate") Date fromDate,
            @Param("toDate") Date toDate
    );

    /**
     * Select tasks by onboarding instance ID with filters and pagination
     * 
     * @param companyId Tenant ID from context
     * @param onboardingId Onboarding instance ID
     * @param status Optional filter by status
     * @param assignedUserId Optional filter by assigned user
     * @param sortBy Sort column (due_date, created_at, status)
     * @param sortOrder Sort direction (ASC, DESC)
     * @param offset Pagination offset
     * @param limit Pagination limit
     * @return List of task instances
     */
    List<TaskInstanceEntity> selectByOnboardingId(
        @Param("companyId") String companyId,
        @Param("onboardingId") String onboardingId,
        @Param("status") String status,
        @Param("assignedUserId") String assignedUserId,
        @Param("sortBy") String sortBy,
        @Param("sortOrder") String sortOrder,
        @Param("offset") Integer offset,
        @Param("limit") Integer limit
    );

    /**
     * Count total tasks by onboarding instance ID with filters
     * 
     * @param companyId Tenant ID from context
     * @param onboardingId Onboarding instance ID
     * @param status Optional filter by status
     * @param assignedUserId Optional filter by assigned user
     * @return Total count
     */
    Integer countByOnboardingId(
        @Param("companyId") String companyId,
        @Param("onboardingId") String onboardingId,
        @Param("status") String status,
        @Param("assignedUserId") String assignedUserId
    );
}
