package com.sme.be_sme.modules.onboarding.support;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

/** Normalizes onboarding instance start dates for SQL {@code DATE} (calendar day, JVM default zone). */
public final class OnboardingInstanceStartDates {

    private OnboardingInstanceStartDates() {}

    public static Date toCalendarDate(Date any) {
        if (any == null) {
            return null;
        }
        LocalDate d = any instanceof java.sql.Date
                ? ((java.sql.Date) any).toLocalDate()
                : any.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        return java.sql.Date.valueOf(d);
    }

    public static Date todayCalendarDate() {
        return java.sql.Date.valueOf(LocalDate.now(ZoneId.systemDefault()));
    }

    /** Client-supplied first day, or today when {@code expectedStartDate} is null. */
    public static Date resolveExpectedOrToday(Date expectedStartDate) {
        return expectedStartDate != null ? toCalendarDate(expectedStartDate) : todayCalendarDate();
    }
}
