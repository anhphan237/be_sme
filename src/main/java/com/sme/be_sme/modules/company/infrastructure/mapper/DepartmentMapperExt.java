package com.sme.be_sme.modules.company.infrastructure.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;

@Mapper
public interface DepartmentMapperExt {

    int updateDepartmentByIdAndCompanyId(
            @Param("departmentId") String departmentId,
            @Param("companyId") String companyId,
            @Param("name") String name,
            @Param("type") String type,
            @Param("status") String status,
            @Param("updatedAt") Date updatedAt
    );
}
