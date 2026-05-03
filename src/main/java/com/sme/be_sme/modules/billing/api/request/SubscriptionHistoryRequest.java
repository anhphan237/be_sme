package com.sme.be_sme.modules.billing.api.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SubscriptionHistoryRequest {
    private String companyId;     // optional, must match tenant if provided
    private String subscriptionId;  // optional, filter one subscription
    private String fromDate;        // optional, yyyy-MM-dd — filters rows whose plan segment overlaps this range
    private String toDate;          // optional, yyyy-MM-dd
    private Integer page;           // optional, default 0
    private Integer size;           // optional, default 20
}
