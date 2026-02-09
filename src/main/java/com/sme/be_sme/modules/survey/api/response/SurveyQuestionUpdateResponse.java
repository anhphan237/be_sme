package com.sme.be_sme.modules.survey.api.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SurveyQuestionUpdateResponse {
    private String questionId;
    private String templateId;

    private String type;
    private String content;
    private Boolean required;
    private Integer sortOrder;

    private String dimensionCode;
    private Boolean measurable;
    private Integer scaleMin;
    private Integer scaleMax;

    private String optionsJson;
}
