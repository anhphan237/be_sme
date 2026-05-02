package com.sme.be_sme.modules.survey.facade;

import com.sme.be_sme.modules.survey.api.request.SurveyManagerEvaluationReportRequest;
import com.sme.be_sme.modules.survey.api.response.SurveyManagerEvaluationReportResponse;
import com.sme.be_sme.shared.gateway.annotation.OperationType;
import com.sme.be_sme.shared.gateway.core.OperationFacadeProvider;

public interface SurveyManagerEvaluationReportFacade extends OperationFacadeProvider {

    @OperationType("com.sme.survey.managerEvaluation.report")
    SurveyManagerEvaluationReportResponse report(SurveyManagerEvaluationReportRequest request);
}
