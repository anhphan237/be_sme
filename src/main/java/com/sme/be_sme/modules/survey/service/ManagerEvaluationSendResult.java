package com.sme.be_sme.modules.survey.service;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ManagerEvaluationSendResult {

    private boolean sent;
    private boolean skipped;

    private String surveyInstanceId;
    private String status;
    private String message;

    public static ManagerEvaluationSendResult sent(String surveyInstanceId) {
        return ManagerEvaluationSendResult.builder()
                .sent(true)
                .skipped(false)
                .surveyInstanceId(surveyInstanceId)
                .status("SENT")
                .message("manager evaluation survey sent")
                .build();
    }

    public static ManagerEvaluationSendResult skipped(String message) {
        return ManagerEvaluationSendResult.builder()
                .sent(false)
                .skipped(true)
                .status("SKIPPED")
                .message(message)
                .build();
    }
}
