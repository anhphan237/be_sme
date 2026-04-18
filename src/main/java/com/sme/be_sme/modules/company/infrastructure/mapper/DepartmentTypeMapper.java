package com.sme.be_sme.modules.company.infrastructure.mapper;

import com.sme.be_sme.modules.company.infrastructure.persistence.entity.DepartmentTypeEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DepartmentTypeMapper {
    int insert(DepartmentTypeEntity row);

    DepartmentTypeEntity selectByPrimaryKey(@Param("departmentTypeId") String departmentTypeId);

    int updateByPrimaryKeySelective(DepartmentTypeEntity row);

    int countByCompany(@Param("companyId") String companyId);

    int countByCompanyAndCode(@Param("companyId") String companyId,
                              @Param("code") String code);

    int countByCompanyAndCodeExcludeId(@Param("companyId") String companyId,
                                       @Param("code") String code,
                                       @Param("departmentTypeId") String departmentTypeId);

    int countByCompanyAndName(@Param("companyId") String companyId,
                              @Param("name") String name);

    int countByCompanyAndNameExcludeId(@Param("companyId") String companyId,
                                       @Param("name") String name,
                                       @Param("departmentTypeId") String departmentTypeId);

    List<DepartmentTypeEntity> selectByCompany(@Param("companyId") String companyId);

    List<DepartmentTypeEntity> selectByCompanyAndStatus(@Param("companyId") String companyId,
                                                        @Param("status") String status);
}
