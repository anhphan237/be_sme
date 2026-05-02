package com.sme.be_sme.modules.analytics.api.request;

import com.sme.be_sme.shared.constant.ErrorCodes;
import com.sme.be_sme.shared.exception.AppException;
import java.util.Locale;
import org.springframework.util.StringUtils;

/** Bucket size for {@link CompanyOnboardingTrendRequest#getGranularity()}. */
public enum OnboardingTrendGranularity {
    /** One row per calendar day; {@code period} is {@code yyyy-MM-dd}. */
    DAY,
    /** One row per calendar month; {@code period} is {@code yyyy-MM}. */
    MONTH,
    /** One row per calendar year; {@code period} is {@code yyyy}. */
    YEAR;

    /**
     * Default {@link #MONTH}. Accepts DAY, MONTH, YEAR (case-insensitive) and aliases DAILY, MONTHLY,
     * YEARLY.
     */
    public static OnboardingTrendGranularity fromPayload(String raw) {
        if (!StringUtils.hasText(raw)) {
            return MONTH;
        }
        String u = raw.trim().toUpperCase(Locale.ROOT);
        return switch (u) {
            case "DAY", "DAILY" -> DAY;
            case "MONTH", "MONTHLY" -> MONTH;
            case "YEAR", "YEARLY" -> YEAR;
            default ->
                    throw AppException.of(
                            ErrorCodes.BAD_REQUEST,
                            "granularity must be DAY, MONTH, or YEAR");
        };
    }
}
