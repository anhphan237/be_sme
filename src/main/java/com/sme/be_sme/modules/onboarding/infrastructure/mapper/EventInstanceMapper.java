package com.sme.be_sme.modules.onboarding.infrastructure.mapper;

import com.sme.be_sme.modules.onboarding.infrastructure.persistence.entity.EventInstanceEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

@Mapper
public interface EventInstanceMapper {
    int insert(EventInstanceEntity row);

    EventInstanceEntity selectByCompanyIdAndEventInstanceId(
            @Param("companyId") String companyId,
            @Param("eventInstanceId") String eventInstanceId
    );

    List<EventInstanceEntity> selectByCompanyIdOrderByEventAtDesc(
            @Param("companyId") String companyId
    );

    List<EventInstanceEntity> selectReadyToNotify(
            @Param("windowStart") Date windowStart,
            @Param("windowEnd") Date windowEnd,
            @Param("limit") int limit
    );

    int markNotifiedIfPending(
            @Param("eventInstanceId") String eventInstanceId,
            @Param("updatedAt") Date updatedAt
    );
    int updateStatusByCompanyIdAndEventInstanceId(
            @Param("companyId") String companyId,
            @Param("eventInstanceId") String eventInstanceId,
            @Param("status") String status,
            @Param("updatedAt") Date updatedAt
    );
}
