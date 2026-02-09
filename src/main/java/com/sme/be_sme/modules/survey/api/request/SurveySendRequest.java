package com.sme.be_sme.modules.survey.api.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SurveySendRequest {
    // 1 trong 2 cách:
    private String surveyInstanceId; // nếu muốn gửi lại / gửi instance đã schedule
    private String templateId;       // nếu gửi ngoài lịch, tạo instance mới

    // optional nhưng nên có (gắn theo onboarding)
    private String onboardingId;
}
