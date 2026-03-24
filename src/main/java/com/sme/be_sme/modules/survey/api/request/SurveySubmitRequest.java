package com.sme.be_sme.modules.survey.api.request;

import java.util.List;
import java.util.Map;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SurveySubmitRequest {
    private String surveyInstanceId;
    private List<AnswerItem> answers;

    @Data
    public static class AnswerItem {
        private String questionId;
        private Object value;
    }
}
