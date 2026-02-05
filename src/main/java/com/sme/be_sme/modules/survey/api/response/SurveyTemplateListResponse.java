package com.sme.be_sme.modules.survey.api.response;

import lombok.Data;

import java.util.List;
@Data
public class SurveyTemplateListResponse {
    private List<SurveyTemplateResponse> items;
    private Integer total;
    private Integer page;
    private Integer pageSize;
}
