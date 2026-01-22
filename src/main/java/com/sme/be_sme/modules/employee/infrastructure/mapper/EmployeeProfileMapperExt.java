package com.sme.be_sme.modules.employee.infrastructure.mapper;

import com.sme.be_sme.modules.employee.infrastructure.persistence.entity.EmployeeProfileEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;

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
}

