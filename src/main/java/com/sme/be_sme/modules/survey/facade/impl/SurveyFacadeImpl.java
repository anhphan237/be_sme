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
    private final SurveyTemplateGetProcessor surveyTemplateGetProcessor;
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
    public SurveyGetResponse getAllSurveys(SurveyGetRequest request) {
        return null;
    }

    @Override
    public SurveyQuestionResponse createSurveyQuestion(SurveyQuestionCreateRequest request) {
        return call(surveyQuestionCreateProcessor, request, SurveyQuestionResponse.class);
    }

    @Override
    public SurveyTemplateDetailResponse getSurveyTemplate(SurveyTemplateGetRequest request) {
        return call(surveyTemplateGetProcessor, request, SurveyTemplateDetailResponse.class);
    }
}
