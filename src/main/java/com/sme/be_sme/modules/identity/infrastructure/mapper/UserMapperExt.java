package com.sme.be_sme.modules.identity.infrastructure.mapper;

import com.sme.be_sme.modules.identity.infrastructure.persistence.entity.UserEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserMapperExt {
    UserEntity selectByCompanyIdAndUserId(@Param("companyId") String companyId,
                                          @Param("userId") String userId);

    List<UserEntity> selectByCompanyId(@Param("companyId") String companyId);

    UserEntity selectByCompanyIdAndEmail(@Param("companyId") String companyId,
                                         @Param("email") String email);

    UserEntity selectByEmail(@Param("email") String email);

    int countActiveByCompanyIdAndUserId(
            @Param("companyId") String companyId,
            @Param("userId") String userId
    );

    int countByEmail(@Param("email") String email);
}
