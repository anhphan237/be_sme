package com.sme.be_sme.modules.onboarding.api.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OnboardingTemplateAIGenerateRequest {
    /** Ngành nghề công ty, ví dụ: Công nghệ, Bán lẻ, Sản xuất, Dịch vụ */
    private String industry;
    /** Quy mô công ty: STARTUP, SME, ENTERPRISE */
    private String companySize;
    /** Vị trí / role của nhân viên mới, ví dụ: Software Engineer, Sales Manager */
    private String jobRole;
}
