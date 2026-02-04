package com.sme.be_sme.modules.survey.api.response;

import lombok.Data;

import java.util.List;

@Data
public class SurveyGetResponse {
    private List<SurveyTemplateResponse> items;
}
