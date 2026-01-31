package com.sme.be_sme.modules.survey.facade.impl;

import com.sme.be_sme.modules.survey.api.request.SurveyGetRequest;
import com.sme.be_sme.modules.survey.api.request.SurveyScheduleRequest;
import com.sme.be_sme.modules.survey.api.request.SurveySubmitRequest;
import com.sme.be_sme.modules.survey.api.request.SurveyTemplateCreateRequest;
import com.sme.be_sme.modules.survey.api.response.SurveyGetResponse;
import com.sme.be_sme.modules.survey.api.response.SurveyScheduleResponse;
import com.sme.be_sme.modules.survey.api.response.SurveySubmitResponse;
import com.sme.be_sme.modules.survey.api.response.SurveyTemplateResponse;
import com.sme.be_sme.modules.survey.facade.SurveyFacade;
import com.sme.be_sme.modules.survey.processor.SurveyGetProcessor;
import com.sme.be_sme.modules.survey.processor.SurveyScheduleProcessor;
import com.sme.be_sme.modules.survey.processor.SurveySubmitProcessor;
import com.sme.be_sme.modules.survey.processor.SurveyTemplateCreateProcessor;
import com.sme.be_sme.shared.gateway.core.BaseOperationFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SurveyFacadeImpl extends BaseOperationFacade implements SurveyFacade {

    private final SurveyTemplateCreateProcessor surveyTemplateCreateProcessor;
    private final SurveyScheduleProcessor surveyScheduleProcessor;
    private final SurveySubmitProcessor surveySubmitProcessor;
    private final SurveyGetProcessor surveyGetProcessor;
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
        return call(surveyGetProcessor, request, SurveyGetResponse.class);
    }
}
