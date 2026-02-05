package com.sme.be_sme.modules.billing.api.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PlanGetResponse {
    private String planId;
    private String code;
    private String name;
    /** e.g. Basic: 5, Pro: 50 employees/month */
    private Integer employeeLimitPerMonth;
    private Integer priceVndMonthly;
    private Integer priceVndYearly;
    private String status;
}
