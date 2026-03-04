package com.sme.be_sme.modules.identity.invite.infrastructure.mapper;

import com.sme.be_sme.modules.identity.invite.infrastructure.entity.UserInviteTokenEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;

@Mapper
public interface UserInviteTokenMapper {
    int insert(UserInviteTokenEntity entity);

    UserInviteTokenEntity selectByTokenHash(@Param("tokenHash") String tokenHash);

    int markUsed(@Param("inviteTokenId") String inviteTokenId, @Param("usedAt") Date usedAt);
}
