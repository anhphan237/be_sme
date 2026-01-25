package com.sme.be_sme.modules.survey.facade;

import com.sme.be_sme.modules.survey.api.request.SurveyScheduleRequest;
import com.sme.be_sme.modules.survey.api.request.SurveySubmitRequest;
import com.sme.be_sme.modules.survey.api.request.SurveyTemplateCreateRequest;
import com.sme.be_sme.modules.survey.api.response.SurveyScheduleResponse;
import com.sme.be_sme.modules.survey.api.response.SurveySubmitResponse;
import com.sme.be_sme.modules.survey.api.response.SurveyTemplateResponse;
import com.sme.be_sme.shared.gateway.annotation.OperationType;
import com.sme.be_sme.shared.gateway.core.OperationFacadeProvider;

public interface SurveyFacade extends OperationFacadeProvider {

    @OperationType("com.sme.survey.template.create")
    SurveyTemplateResponse createSurveyTemplate(SurveyTemplateCreateRequest request);

    @OperationType("com.sme.survey.instance.schedule")
    SurveyScheduleResponse scheduleSurvey(SurveyScheduleRequest request);

    @OperationType("com.sme.survey.response.submit")
    SurveySubmitResponse submitSurveyResponse(SurveySubmitRequest request);
}
