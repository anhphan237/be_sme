package com.sme.be_sme.modules.company.api.request;

import lombok.Getter;
import lombok.Setter;

/**
 * Self-registration creates an ACTIVE FREE subscription; if {@code planCode} is a paid global plan,
 * a pending subscription change + ISSUED invoice is created so the tenant appears FREE until Stripe payment succeeds.
 */
@Getter
@Setter
public class CompanyRegisterRequest {
    private CompanyInfo company;
    private AdminInfo admin;
    /** Optional. Paid global plan to activate after payment; omit or FREE to stay FREE without an upgrade invoice. */
    private String planCode;
    /** Optional MONTHLY/YEARLY for the eventual paid plan and initial FREE period cadence (default MONTHLY). */
    private String billingCycle;

    @Getter
    @Setter
    public static class CompanyInfo {
        private String name;
        private String taxCode;
        /** Company code (3 chars) for employee format [ABC]000001. Optional; derived from name if null. */
        private String code;
        private String address;
        private String timezone;
    }

    @Getter
    @Setter
    public static class AdminInfo {
        private String username;
        private String password;
        private String fullName;
        private String phone;
    }
}
