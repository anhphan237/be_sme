package com.sme.be_sme.modules.survey.infrastructure.mapper;


import com.sme.be_sme.modules.survey.infrastructure.persistence.entity.SurveyTemplateEntity;
import com.sme.be_sme.modules.survey.infrastructure.persistence.model.ManagerEvaluationTemplateRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface SurveyTemplateMapperExt {

    SurveyTemplateEntity selectByIdAndCompanyId(@Param("surveyTemplateId") String surveyTemplateId,
                                                @Param("companyId") String companyId);

    boolean existsAnyInstanceByTemplateId(@Param("surveyTemplateId") String surveyTemplateId,
                                          @Param("companyId") String companyId);

    boolean existsSentInstanceByTemplateId(@Param("surveyTemplateId") String surveyTemplateId,
                                           @Param("companyId") String companyId);

    int deleteByIdAndCompanyId(@Param("surveyTemplateId") String surveyTemplateId,
                               @Param("companyId") String companyId);

    ManagerEvaluationTemplateRow selectDefaultManagerEvaluationTemplate(
            @Param("companyId") String companyId
    );

    ManagerEvaluationTemplateRow selectManagerEvaluationTemplateById(
            @Param("companyId") String companyId,
            @Param("surveyTemplateId") String surveyTemplateId
    );
    int countManagerEvaluationTemplates(@Param("companyId") String companyId);
    SurveyTemplateEntity findActiveDefaultByCompanyStageAndTargetRole(
            @Param("companyId") String companyId,
            @Param("stage") String stage,
            @Param("targetRole") String targetRole
    );

    SurveyTemplateEntity findActiveDefaultByCompanyStageAndTargetRoleExcludingTemplateId(
            @Param("companyId") String companyId,
            @Param("stage") String stage,
            @Param("targetRole") String targetRole,
            @Param("excludeTemplateId") String excludeTemplateId
    );

    int clearDefaultByCompanyStageAndTargetRoleExcludingTemplateId(
            @Param("companyId") String companyId,
            @Param("stage") String stage,
            @Param("targetRole") String targetRole,
            @Param("excludeTemplateId") String excludeTemplateId
    );
}