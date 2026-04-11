package com.sme.be_sme.modules.survey.infrastructure.mapper;


import com.sme.be_sme.modules.survey.infrastructure.persistence.entity.SurveyTemplateEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface SurveyTemplateMapperExt {

    SurveyTemplateEntity selectByIdAndCompanyId(@Param("surveyTemplateId") String surveyTemplateId,
                                                @Param("companyId") String companyId);

    boolean existsAnyInstanceByTemplateId(@Param("surveyTemplateId") String surveyTemplateId,
                                          @Param("companyId") String companyId);

    int deleteByIdAndCompanyId(@Param("surveyTemplateId") String surveyTemplateId,
                               @Param("companyId") String companyId);
}