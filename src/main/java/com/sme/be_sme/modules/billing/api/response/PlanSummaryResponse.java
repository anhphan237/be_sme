package com.sme.be_sme.modules.billing.api.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PlanSummaryResponse {
    private String planId;
    private String code;
    private String name;
    private Integer employeeLimitPerMonth;
    private Integer priceVndMonthly;
    private Integer priceVndYearly;
    private String status;
}
