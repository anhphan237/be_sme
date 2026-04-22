package com.sme.be_sme.modules.platform.infrastructure.mapper;

import com.sme.be_sme.modules.platform.infrastructure.persistence.entity.ActivityLogEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ActivityLogMapper {
    int insert(ActivityLogEntity row);
    ActivityLogEntity selectByPrimaryKey(String logId);
    List<ActivityLogEntity> selectAll();
    List<ActivityLogEntity> selectByUserIdWithPaging(
            @Param("userId") String userId,
            @Param("offset") int offset,
            @Param("limit") int limit);
    int countByUserId(@Param("userId") String userId);
    List<ActivityLogEntity> selectPage(
            @Param("offset") int offset,
            @Param("limit") int limit);
    int countAll();
}
