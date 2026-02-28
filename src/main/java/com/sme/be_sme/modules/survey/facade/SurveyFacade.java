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

    @OperationType("com.sme.survey.instance.list")
    SurveyInstanceListResponse listSurveyInstances(SurveyInstanceListRequest request);

    @OperationType("com.sme.survey.report.satisfaction")
    SurveySatisfactionReportResponse getSatisfactionReport(SurveySatisfactionReportRequest request);

    @OperationType("com.sme.survey.instance.send")
    SurveySendResponse sendSurvey(SurveySendRequest request);

    @OperationType("com.sme.survey.response.list")
    SurveyResponseListResponse listSurveyResponses(SurveyResponseListRequest request);

    @OperationType("com.sme.survey.analytics.report")
    SurveyAnalyticsReportResponse getSurveyAnalyticsReport(SurveyAnalyticsReportRequest request);

}
