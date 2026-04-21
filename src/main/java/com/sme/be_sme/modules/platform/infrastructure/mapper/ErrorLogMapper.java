package com.sme.be_sme.modules.platform.infrastructure.mapper;

import com.sme.be_sme.modules.platform.infrastructure.persistence.entity.ErrorLogEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ErrorLogMapper {

    void insert(ErrorLogEntity entity);

    List<ErrorLogEntity> selectPage(
            @Param("keyword") String keyword,
            @Param("errorCode") String errorCode,
            @Param("severity") String severity,
            @Param("status") String status,
            @Param("operationType") String operationType,
            @Param("companyId") String companyId,
            @Param("actorRole") String actorRole,
            @Param("startDate") String startDate,
            @Param("endDate") String endDate,
            @Param("limit") int limit,
            @Param("offset") int offset
    );

    int countPage(
            @Param("keyword") String keyword,
            @Param("errorCode") String errorCode,
            @Param("severity") String severity,
            @Param("status") String status,
            @Param("operationType") String operationType,
            @Param("companyId") String companyId,
            @Param("actorRole") String actorRole,
            @Param("startDate") String startDate,
            @Param("endDate") String endDate
    );

    List<ErrorLogEntity> selectAll();

    ErrorLogEntity selectById(@Param("errorId") String errorId);

    int updateStatus(
            @Param("errorId") String errorId,
            @Param("status") String status,
            @Param("resolvedBy") String resolvedBy,
            @Param("resolutionNote") String resolutionNote
    );
}