package com.sme.be_sme.modules.survey.infrastructure.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface SurveyManagerEvaluationReportMapper {

    List<Map<String, Object>> selectEvaluationRows(
            @Param("companyId") String companyId,
            @Param("templateId") String templateId,
            @Param("startDate") String startDate,
            @Param("endDate") String endDate,
            @Param("managerUserId") String managerUserId,
            @Param("keyword") String keyword,
            @Param("status") String status
    );

    List<Map<String, Object>> selectAnswerRows(
            @Param("companyId") String companyId,
            @Param("templateId") String templateId,
            @Param("startDate") String startDate,
            @Param("endDate") String endDate,
            @Param("managerUserId") String managerUserId,
            @Param("keyword") String keyword,
            @Param("status") String status
    );
}
