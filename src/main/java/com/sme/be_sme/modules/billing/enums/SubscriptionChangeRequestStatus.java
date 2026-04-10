package com.sme.be_sme.modules.billing.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum SubscriptionChangeRequestStatus {
    PENDING_PAYMENT("PENDING_PAYMENT"),
    APPLIED("APPLIED"),
    FAILED("FAILED"),
    CANCELLED("CANCELLED");

    private final String code;
}
