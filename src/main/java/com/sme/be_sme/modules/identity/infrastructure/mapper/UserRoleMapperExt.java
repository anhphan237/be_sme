package com.sme.be_sme.modules.identity.infrastructure.mapper;

import com.sme.be_sme.modules.identity.infrastructure.persistence.entity.UserRoleEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

@Mapper
public interface UserRoleMapperExt {
    int countByCompanyIdAndUserIdAndRoleId(@Param("companyId") String companyId,
                                           @Param("userId") String userId,
                                           @Param("roleId") String roleId);

    int insert(UserRoleEntity entity);

    int insertUserRole(
            @Param("userRoleId") String userRoleId,
            @Param("companyId") String companyId,
            @Param("userId") String userId,
            @Param("roleId") String roleId,
            @Param("createdAt") Date createdAt
    );

    String selectOneRoleIdByCompanyAndUser(
            @Param("companyId") String companyId,
            @Param("userId") String userId
    );

    List<String> selectRoleCodesByCompanyAndUser(
            @Param("companyId") String companyId,
            @Param("userId") String userId
    );

    int updateRoleForUser(
            @Param("companyId") String companyId,
            @Param("userId") String userId,
            @Param("oldRoleId") String oldRoleId,
            @Param("newRoleId") String newRoleId,
            @Param("updatedAt") Date updatedAt
    );
}

