package com.sme.be_sme.modules.billing.api.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SubscriptionHistoryRequest {
    private String companyId; // optional, must match tenant if provided
    private String fromDate;  // optional, yyyy-MM-dd
    private String toDate;    // optional, yyyy-MM-dd
    private Integer page;     // optional, default 0
    private Integer size;     // optional, default 20
}
