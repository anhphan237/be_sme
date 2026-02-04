package com.sme.be_sme.modules.survey.facade;

import com.sme.be_sme.modules.survey.api.request.*;
import com.sme.be_sme.modules.survey.api.response.*;
import com.sme.be_sme.shared.gateway.annotation.OperationType;
import com.sme.be_sme.shared.gateway.core.OperationFacadeProvider;

public interface SurveyFacade extends OperationFacadeProvider {

    @OperationType("com.sme.survey.template.create")
    SurveyTemplateResponse createSurveyTemplate(SurveyTemplateCreateRequest request);

    @OperationType("com.sme.survey.instance.schedule")
    SurveyScheduleResponse scheduleSurvey(SurveyScheduleRequest request);

    @OperationType("com.sme.survey.response.submit")
    SurveySubmitResponse submitSurveyResponse(SurveySubmitRequest request);

    @OperationType("com.sme.survey.response.get")
    SurveyGetResponse getAllSurveys(SurveyGetRequest request);

    @OperationType("com.sme.survey.question.create")
    SurveyQuestionResponse createSurveyQuestion(SurveyQuestionCreateRequest request);

    @OperationType("com.sme.survey.template.get")
    SurveyTemplateDetailResponse getSurveyTemplate(SurveyTemplateGetRequest request);
}
