package com.sme.be_sme.modules.identity.infrastructure.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface RoleMapperExt {
    String selectRoleIdByCode(@Param("companyId") String companyId,
                              @Param("code") String code);

    String selectRoleIdByCompanyIdAndCode(
            @Param("companyId") String companyId,
            @Param("code") String code
    );
}
