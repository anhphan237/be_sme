package com.sme.be_sme.modules.platform.api.request;

import lombok.Data;

@Data
public class PlatformPlanUpdateRequest {
    private String planId;
    private String name;
    private Integer employeeLimitPerMonth;
    private Integer onboardingTemplateLimit;
    private Integer eventTemplateLimit;
    private Integer documentLimit;
    private Long storageLimitBytes;
    private Integer priceVndMonthly;
    private Integer priceVndYearly;
}
