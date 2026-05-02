package com.sme.be_sme.modules.analytics.api.request;

import lombok.Getter;
import lombok.Setter;

/**
 * Time range for onboarding trend (line chart). Dates are ISO {@code yyyy-MM-dd}.
 * Bucket series is controlled by {@link #granularity}: each bucket between {@link #startDate} and
 * {@link #endDate} inclusive (by calendar day / month / year as applicable).
 */
@Getter
@Setter
public class CompanyOnboardingTrendRequest {
    /** Optional; must match JWT tenant unless platform admin. */
    private String companyId;
    private String startDate;
    private String endDate;
    /**
     * {@code DAY}, {@code MONTH}, or {@code YEAR}; optional, default {@code MONTH}. Aliases:
     * DAILY, MONTHLY, YEARLY.
     */
    private String granularity;
}
