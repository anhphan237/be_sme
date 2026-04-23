package com.sme.be_sme.modules.survey.infrastructure.mapper;

import com.sme.be_sme.modules.survey.infrastructure.persistence.entity.SurveyAiSummaryEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.Date;

@Mapper
public interface SurveyAiSummaryMapper {

    SurveyAiSummaryEntity selectByCacheKey(
            @Param("companyId") String companyId,
            @Param("templateId") String templateId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("language") String language,
            @Param("inputHash") String inputHash
    );

    int countGeneratedSince(
            @Param("companyId") String companyId,
            @Param("generatedBy") String generatedBy,
            @Param("since") Date since
    );

    int insert(SurveyAiSummaryEntity entity);

    int updateBySummaryId(SurveyAiSummaryEntity entity);
}