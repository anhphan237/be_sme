package com.sme.be_sme.modules.survey.api.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SurveyQuestionUpdateRequest {
    private String questionId;

    private String content;
    private Boolean required;
    private Integer sortOrder;

    private String dimensionCode;
    private Boolean measurable;
    private Integer scaleMin;
    private Integer scaleMax;

    private String optionsJson;
}
