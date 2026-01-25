package com.sme.be_sme.modules.survey.facade.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.sme.be_sme.modules.survey.facade.SurveyFacade;
import com.sme.be_sme.shared.gateway.api.OperationStubResponse;
import org.springframework.stereotype.Component;

@Component
public class SurveyFacadeImpl implements SurveyFacade {

    @Override
    public OperationStubResponse createSurveyTemplate(JsonNode payload) {
        return OperationStubResponse.notImplemented("com.sme.survey.template.create");
    }

    @Override
    public OperationStubResponse scheduleSurvey(JsonNode payload) {
        return OperationStubResponse.notImplemented("com.sme.survey.instance.schedule");
    }

    @Override
    public OperationStubResponse submitSurveyResponse(JsonNode payload) {
        return OperationStubResponse.notImplemented("com.sme.survey.response.submit");
    }
}
