package com.sme.be_sme.modules.billing.api.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UsageCheckRequest {
    /** Optional: yyyy-MM. If not provided, current month is used. */
    private String month;
}
