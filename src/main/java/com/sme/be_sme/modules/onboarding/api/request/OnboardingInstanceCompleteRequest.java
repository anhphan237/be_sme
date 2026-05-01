package com.sme.be_sme.modules.onboarding.api.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OnboardingInstanceCompleteRequest {

    private String instanceId;
    /**
     * Optional.
     * Nếu không truyền, BE tự lấy template mặc định:
     * purpose = MANAGER_EVALUATION
     * stage = COMPLETED
     * target_role = MANAGER
     * is_default = true
     */
    private String managerEvaluationTemplateId;

    /**
     * Optional.
     * Default = 7.
     */
    private Integer managerEvaluationDueDays;

    /**
     * Optional.
     * Default = false.
     * Chỉ dùng nếu HR muốn hoàn tất onboarding nhưng không gửi đánh giá manager.
     */
    private Boolean skipManagerEvaluation;
}
