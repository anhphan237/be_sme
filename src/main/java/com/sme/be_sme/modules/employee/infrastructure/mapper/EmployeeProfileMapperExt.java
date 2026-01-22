package com.sme.be_sme.modules.employee.infrastructure.mapper;

import com.sme.be_sme.modules.employee.infrastructure.persistence.entity.EmployeeProfileEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface EmployeeProfileMapperExt {

    EmployeeProfileEntity selectByCompanyIdAndUserId(@Param("companyId") String companyId,
                                                     @Param("userId") String userId);

    int insert(EmployeeProfileEntity entity);

    int updateSelectiveByEmployeeId(EmployeeProfileEntity entity);
}

