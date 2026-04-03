package com.sme.be_sme.modules.platform.infrastructure.mapper;

import com.sme.be_sme.modules.platform.infrastructure.persistence.entity.ActivityLogEntity;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ActivityLogMapper {
    int insert(ActivityLogEntity row);
    ActivityLogEntity selectByPrimaryKey(String logId);
    List<ActivityLogEntity> selectAll();
}
