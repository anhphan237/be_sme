package com.sme.be_sme.modules.survey.facade.impl;

import com.sme.be_sme.modules.survey.api.request.*;
import com.sme.be_sme.modules.survey.api.response.*;
import com.sme.be_sme.modules.survey.facade.SurveyFacade;
import com.sme.be_sme.modules.survey.processor.*;
import com.sme.be_sme.shared.gateway.core.BaseOperationFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SurveyFacadeImpl extends BaseOperationFacade implements SurveyFacade {

    private final SurveyTemplateCreateProcessor surveyTemplateCreateProcessor;
    private final SurveyScheduleProcessor surveyScheduleProcessor;
    private final SurveySubmitProcessor surveySubmitProcessor;
    private final SurveyQuestionCreateProcessor surveyQuestionCreateProcessor;
    private final SurveyTemplateGetDetailProcessor surveyTemplateGetDetailProcessor;
    private final SurveyTemplateGetListProcessor surveyTemplateGetListProcessor;
    private final SurveyTemplateUpdateProcessor surveyTemplateUpdateProcessor;
    private final SurveyTemplateArchiveProcessor surveyTemplateArchiveProcessor;
    private final SurveyQuestionGetByTemplateProcessor surveyQuestionGetByTemplateProcessor;
    private final SurveyInstanceListProcessor surveyInstanceListProcessor;
    private final SurveySatisfactionReportProcessor surveySatisfactionReportProcessor;
    private final SurveyInstanceSendProcessor surveyInstanceSendProcessor;
    private final SurveyResponseListProcessor surveyResponseListProcessor;
    @Override
    public SurveyTemplateResponse createSurveyTemplate(SurveyTemplateCreateRequest request) {
        return call(surveyTemplateCreateProcessor, request, SurveyTemplateResponse.class);
    }

    @Override
    public SurveyScheduleResponse scheduleSurvey(SurveyScheduleRequest request) {
        return call(surveyScheduleProcessor, request, SurveyScheduleResponse.class);
    }

    @Override
    public SurveySubmitResponse submitSurveyResponse(SurveySubmitRequest request) {
        return call(surveySubmitProcessor, request, SurveySubmitResponse.class);
    }

    @Override
    public SurveyQuestionResponse createSurveyQuestion(SurveyQuestionCreateRequest request) {
        return call(surveyQuestionCreateProcessor, request, SurveyQuestionResponse.class);
    }

    @Override
    public SurveyQuestionListResponse getSurveyQuestionListByTemplate(SurveyQuestionGetByTemplateRequest request) {
        return call(surveyQuestionGetByTemplateProcessor, request, SurveyQuestionListResponse.class);
    }

    @Override
    public SurveyTemplateDetailResponse getSurveyDetailTemplate(SurveyTemplateGetRequest request) {
        return call(surveyTemplateGetDetailProcessor, request, SurveyTemplateDetailResponse.class);
    }

    @Override
    public SurveyTemplateListResponse listSurveyTemplates(SurveyTemplateGetListRequest request) {
        return call(surveyTemplateGetListProcessor, request, SurveyTemplateListResponse.class);
    }

    @Override
    public SurveyTemplateResponse updateSurveyTemplate(SurveyTemplateUpdateRequest request) {
        return call(surveyTemplateUpdateProcessor, request, SurveyTemplateResponse.class);
    }

    @Override
    public SurveyTemplateArchiveResponse archiveSurveyTemplate(SurveyTemplateArchiveRequest request) {
        return call(surveyTemplateArchiveProcessor, request, SurveyTemplateArchiveResponse.class);
    }

    @Override
    public SurveyInstanceListResponse listSurveyInstances(SurveyInstanceListRequest request) {
        return call(surveyInstanceListProcessor, request, SurveyInstanceListResponse.class);
    }

    @Override
    public SurveySatisfactionReportResponse getSatisfactionReport(SurveySatisfactionReportRequest request) {
        return call(surveySatisfactionReportProcessor, request, SurveySatisfactionReportResponse.class);
    }

    @Override
    public SurveySendResponse sendSurvey(SurveySendRequest request) {
        return call(surveyInstanceSendProcessor, request, SurveySendResponse.class);
    }

    @Override
    public SurveyResponseListResponse listSurveyResponses(SurveyResponseListRequest request) {
        return call(surveyResponseListProcessor, request, SurveyResponseListResponse.class);
    }

}
