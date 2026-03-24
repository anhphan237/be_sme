package com.sme.be_sme.modules.survey.api.request;

import lombok.Data;

import java.util.List;
@Data
public class SurveyResponseSaveDraftRequest {
    private String instanceId;
    private List<AnswerItem> answers;

    @Data
    public static class AnswerItem {
        private String questionId;
        private Object value;
    }
}
