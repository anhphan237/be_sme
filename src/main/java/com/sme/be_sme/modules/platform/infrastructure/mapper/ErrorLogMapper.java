package com.sme.be_sme.modules.platform.infrastructure.mapper;

import com.sme.be_sme.modules.platform.infrastructure.persistence.entity.ErrorLogEntity;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ErrorLogMapper {
    int insert(ErrorLogEntity row);
    ErrorLogEntity selectByPrimaryKey(String errorId);
    List<ErrorLogEntity> selectAll();
}
