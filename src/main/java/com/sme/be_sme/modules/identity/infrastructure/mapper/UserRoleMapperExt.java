package com.sme.be_sme.modules.identity.infrastructure.mapper;

import com.sme.be_sme.modules.identity.infrastructure.persistence.entity.UserRoleEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserRoleMapperExt {
    int countByCompanyIdAndUserIdAndRoleId(@Param("companyId") String companyId,
                                           @Param("userId") String userId,
                                           @Param("roleId") String roleId);

    int insert(UserRoleEntity entity);
}

