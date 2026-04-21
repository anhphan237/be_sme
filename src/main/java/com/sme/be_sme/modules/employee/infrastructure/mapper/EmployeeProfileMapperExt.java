package com.sme.be_sme.modules.employee.infrastructure.mapper;

import com.sme.be_sme.modules.employee.infrastructure.persistence.entity.EmployeeProfileEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

@Mapper
public interface EmployeeProfileMapperExt {

    EmployeeProfileEntity selectByCompanyIdAndUserId(@Param("companyId") String companyId,
                                                     @Param("userId") String userId);

    int insert(EmployeeProfileEntity entity);

    void updateSelectiveByEmployeeId(EmployeeProfileEntity entity);

    int updateStatusByCompanyIdAndUserId(
            @Param("companyId") String companyId,
            @Param("userId") String userId,
            @Param("status") String status,
            @Param("updatedAt") Date updatedAt
    );

    /**
     * Returns next employee sequence (1-based) for format [XXX]NNNNNN.
     * Scans existing employee_code values ending with digits after ']'.
     */
    int getNextEmployeeSequence(@Param("companyId") String companyId);

    List<String> selectActiveUserIdsByDepartmentIds(
            @Param("companyId") String companyId,
            @Param("departmentIds") List<String> departmentIds
    );
}

