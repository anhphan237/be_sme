package com.sme.be_sme.modules.survey.api.response;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class SurveyInstanceGetResponse {
    private String instanceId;
    private String templateId;
    private String templateName;
    private String status;
    private LocalDateTime scheduledAt;
    private List<SurveyDraftAnswerDto> draftAnswers;

    @Data
    public static class SurveyDraftAnswerDto {
        private String questionId;
        private Object value;
    }
}

