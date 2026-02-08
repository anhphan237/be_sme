package com.sme.be_sme.modules.survey.facade;

import com.sme.be_sme.modules.survey.api.request.*;
import com.sme.be_sme.modules.survey.api.response.*;
import com.sme.be_sme.shared.gateway.annotation.OperationType;
import com.sme.be_sme.shared.gateway.core.OperationFacadeProvider;

import java.util.Map;

public interface SurveyFacade extends OperationFacadeProvider {

    @OperationType("com.sme.survey.template.create")
    SurveyTemplateResponse createSurveyTemplate(SurveyTemplateCreateRequest request);

    @OperationType("com.sme.survey.instance.schedule")
    SurveyScheduleResponse scheduleSurvey(SurveyScheduleRequest request);

    @OperationType("com.sme.survey.response.submit")
    SurveySubmitResponse submitSurveyResponse(SurveySubmitRequest request);

    @OperationType("com.sme.survey.question.create")
    SurveyQuestionResponse createSurveyQuestion(SurveyQuestionCreateRequest request);

    @OperationType("com.sme.survey.question.list.bytemplate")
    SurveyQuestionListResponse getSurveyQuestionListByTemplate(SurveyQuestionGetByTemplateRequest request);

    @OperationType("com.sme.survey.template.get")
    SurveyTemplateDetailResponse getSurveyDetailTemplate(SurveyTemplateGetRequest request);

    @OperationType("com.sme.survey.template.list")
    SurveyTemplateListResponse listSurveyTemplates(SurveyTemplateGetListRequest request);

    @OperationType("com.sme.survey.template.update")
    SurveyTemplateResponse updateSurveyTemplate(SurveyTemplateUpdateRequest request);

    @OperationType("com.sme.survey.template.archive")
    SurveyTemplateArchiveResponse archiveSurveyTemplate(SurveyTemplateArchiveRequest request);

    @OperationType("com.sme.survey.question.update")
    SurveyQuestionUpdateResponse updateQuestion(SurveyQuestionUpdateRequest request);

    @OperationType("com.sme.survey.question.delete")
    Map<String, Object>  deleteQuestion(SurveyQuestionDeleteRequest request);

}
