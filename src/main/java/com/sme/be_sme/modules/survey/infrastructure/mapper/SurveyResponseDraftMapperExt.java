package com.sme.be_sme.modules.survey.infrastructure.mapper;

import com.sme.be_sme.modules.survey.infrastructure.persistence.model.SurveyInstanceDetailRow;
import com.sme.be_sme.modules.survey.infrastructure.persistence.model.SurveyResponseDraftRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SurveyResponseDraftMapperExt {
    void upsertDraft(
            @Param("draftId") String draftId,
            @Param("companyId") String companyId,
            @Param("instanceId") String instanceId,
            @Param("questionId") String questionId,
            @Param("responderUserId") String responderUserId,
            @Param("answerValue") String answerValue
    );

    List<SurveyResponseDraftRow> selectByInstanceIdAndResponder(
            @Param("companyId") String companyId,
            @Param("instanceId") String instanceId,
            @Param("responderUserId") String responderUserId
    );

    int deleteByInstanceIdAndResponder(
            @Param("companyId") String companyId,
            @Param("instanceId") String instanceId,
            @Param("responderUserId") String responderUserId
    );

    SurveyInstanceDetailRow selectDetailById(
            @Param("companyId") String companyId,
            @Param("instanceId") String instanceId,
            @Param("responderUserId") String responderUserId
    );
}
