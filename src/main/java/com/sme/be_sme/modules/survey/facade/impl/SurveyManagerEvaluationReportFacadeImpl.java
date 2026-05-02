package com.sme.be_sme.modules.survey.facade.impl;

import com.sme.be_sme.modules.survey.api.request.SurveyManagerEvaluationReportRequest;
import com.sme.be_sme.modules.survey.api.response.SurveyManagerEvaluationReportResponse;
import com.sme.be_sme.modules.survey.facade.SurveyManagerEvaluationReportFacade;
import com.sme.be_sme.modules.survey.processor.SurveyManagerEvaluationReportProcessor;
import com.sme.be_sme.shared.gateway.core.BaseOperationFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SurveyManagerEvaluationReportFacadeImpl extends BaseOperationFacade implements SurveyManagerEvaluationReportFacade {

    private final SurveyManagerEvaluationReportProcessor surveyManagerEvaluationReportProcessor;

    @Override
    public SurveyManagerEvaluationReportResponse report(SurveyManagerEvaluationReportRequest request) {
        return call(
                surveyManagerEvaluationReportProcessor,
                request,
                SurveyManagerEvaluationReportResponse.class
        );
    }
}
