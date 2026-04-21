package com.sme.be_sme.modules.onboarding.infrastructure.mapper;

import com.sme.be_sme.modules.onboarding.infrastructure.persistence.entity.EventInstanceEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

@Mapper
public interface EventInstanceMapper {
    int insert(EventInstanceEntity row);

    List<EventInstanceEntity> selectReadyToNotify(
            @Param("windowStart") Date windowStart,
            @Param("windowEnd") Date windowEnd,
            @Param("limit") int limit
    );

    int markNotifiedIfPending(
            @Param("eventInstanceId") String eventInstanceId,
            @Param("updatedAt") Date updatedAt
    );
}
