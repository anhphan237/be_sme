package com.sme.be_sme.modules.survey.infrastructure.mapper;

import com.sme.be_sme.modules.survey.infrastructure.persistence.entity.SurveyInstanceEntity;
import com.sme.be_sme.modules.survey.infrastructure.persistence.model.SurveyInstanceDetailRow;
import com.sme.be_sme.modules.survey.infrastructure.persistence.model.SurveyInstanceListRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Date;
import java.util.List;

@Mapper
public interface SurveyInstanceMapperExt {

    List<SurveyInstanceListRow> selectListByCompanyId(
            @Param("companyId") String companyId,
            @Param("templateId") String templateId,
            @Param("status") String status,
            @Param("startDate") Date startDate,
            @Param("endDate") Date endDate,
            @Param("responderUserId") String responderUserId,
            @Param("offset") int offset,
            @Param("limit") int limit
    );

    int countByCompanyId(
            @Param("companyId") String companyId,
            @Param("templateId") String templateId,
            @Param("status") String status,
            @Param("startDate") Date startDate,
            @Param("endDate") Date endDate,
            @Param("responderUserId") String responderUserId
    );

    int countSent(
            @Param("companyId") String companyId,
            @Param("templateId") String templateId,
            @Param("startDate") Date startDate,
            @Param("endDate") Date endDate
    );
    SurveyInstanceDetailRow selectDetailById(
            @Param("companyId") String companyId,
            @Param("instanceId") String instanceId,
            @Param("responderUserId") String responderUserId
    );
    SurveyInstanceEntity findActiveByUniqueKey(
            @Param("companyId") String companyId,
            @Param("onboardingId") String onboardingId,
            @Param("templateId") String templateId,
            @Param("responderUserId") String responderUserId
    );

    List<SurveyInstanceEntity> selectScheduledReadyToDispatch(
            @Param("now") Date now,
            @Param("limit") Integer limit
    );

    int markAsSentIfScheduled(
            @Param("surveyInstanceId") String surveyInstanceId,
            @Param("sentAt") Date sentAt
    );
    List<SurveyInstanceEntity> selectExpiredOpenInstances(
            @Param("now") Date now,
            @Param("limit") int limit
    );
    SurveyInstanceEntity findExistingManagerEvaluation(
            @Param("companyId") String companyId,
            @Param("onboardingId") String onboardingId,
            @Param("surveyTemplateId") String surveyTemplateId,
            @Param("responderUserId") String responderUserId,
            @Param("subjectUserId") String subjectUserId
    );

    int insertManagerEvaluationInstance(
            @Param("surveyInstanceId") String surveyInstanceId,
            @Param("companyId") String companyId,
            @Param("onboardingId") String onboardingId,
            @Param("surveyTemplateId") String surveyTemplateId,
            @Param("scheduledAt") Date scheduledAt,
            @Param("sentAt") Date sentAt,
            @Param("closedAt") Date closedAt,
            @Param("status") String status,
            @Param("createdAt") Date createdAt,
            @Param("responderUserId") String responderUserId,
            @Param("updatedAt") Date updatedAt,
            @Param("purpose") String purpose,
            @Param("subjectUserId") String subjectUserId
    );
}
