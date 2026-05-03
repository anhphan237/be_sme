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
    /** optional; e.g. 2025 — restricted to that calendar year, intersected with fromDate/toDate if also set */
    private Integer year;
    private Integer page;           // optional, default 0
    private Integer size;           // optional, default 20
}
