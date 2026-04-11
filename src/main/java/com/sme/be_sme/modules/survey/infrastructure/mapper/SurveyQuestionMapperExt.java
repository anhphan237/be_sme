package com.sme.be_sme.modules.survey.infrastructure.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface SurveyQuestionMapperExt {
    int deleteByTemplateId(@Param("surveyTemplateId") String surveyTemplateId,
                           @Param("companyId") String companyId);
}
