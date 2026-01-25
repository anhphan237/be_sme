package com.sme.be_sme.modules.survey.facade;

import com.fasterxml.jackson.databind.JsonNode;
import com.sme.be_sme.shared.gateway.annotation.OperationType;
import com.sme.be_sme.shared.gateway.api.OperationStubResponse;
import com.sme.be_sme.shared.gateway.core.OperationFacadeProvider;

public interface SurveyFacade extends OperationFacadeProvider {

    @OperationType("com.sme.survey.template.create")
    OperationStubResponse createSurveyTemplate(JsonNode payload);

    @OperationType("com.sme.survey.instance.schedule")
    OperationStubResponse scheduleSurvey(JsonNode payload);

    @OperationType("com.sme.survey.response.submit")
    OperationStubResponse submitSurveyResponse(JsonNode payload);
}
