package com.sme.be_sme.modules.survey.api.request;

import lombok.Data;

@Data
public class SurveyQuestionCreateRequest {
    private String templateId;
    private String type;
    private String content;
    private Boolean required;
    private Integer sortOrder;
    private String optionsJson;
    private String dimensionCode;
    private Boolean measurable;
    private Integer scaleMin;
    private Integer scaleMax;
}
