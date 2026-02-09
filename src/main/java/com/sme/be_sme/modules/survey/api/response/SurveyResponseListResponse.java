package com.sme.be_sme.modules.survey.api.response;

import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
public class SurveyResponseListResponse {
    private List<SurveyResponseItem> items;
}
