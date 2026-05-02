package com.sme.be_sme.modules.analytics.api.response;

import lombok.Getter;
import lombok.Setter;

/** One time bucket for onboarding trend (line chart x-axis). */
@Getter
@Setter
public class CompanyOnboardingTrendPoint {
    /**
     * Bucket label / key, depending on request granularity:
     * <ul>
     *   <li>{@code DAY} — {@code yyyy-MM-dd}
     *   <li>{@code MONTH} — {@code yyyy-MM}
     *   <li>{@code YEAR} — {@code yyyy}
     * </ul>
     */
    private String period;
    private int createdCount;
    private int completedCount;
    private int cancelledCount;
}
