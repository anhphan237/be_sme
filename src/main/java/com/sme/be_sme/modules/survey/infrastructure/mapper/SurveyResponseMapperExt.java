package com.sme.be_sme.modules.survey.infrastructure.mapper;

import com.sme.be_sme.modules.survey.infrastructure.persistence.model.SurveyResponseFilterRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

@Mapper
public interface SurveyResponseMapperExt {

    /**
     * Select responses by company and filters (date range, optional templateId, optional stage 7/30/60).
     * Joins survey_instances and survey_templates to filter by template and stage.
     */
    List<SurveyResponseFilterRow> selectByCompanyIdAndFilters(
            @Param("companyId") String companyId,
            @Param("startDate") Date startDate,
            @Param("endDate") Date endDate,
            @Param("templateId") String templateId,
            @Param("stage") String stage
    );
}
