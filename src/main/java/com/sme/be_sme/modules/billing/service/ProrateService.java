package com.sme.be_sme.modules.billing.service;

import com.sme.be_sme.modules.billing.infrastructure.persistence.entity.PlanEntity;
import com.sme.be_sme.modules.billing.infrastructure.persistence.entity.SubscriptionEntity;
import lombok.Getter;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Date;

/**
 * Calculates prorated credit/charge when subscription plan is changed mid-period.
 * Upgrade: charge prorated difference for remaining days. Downgrade: credit prorated unused amount.
 */
@Service
public class ProrateService {

    /**
     * @param subscription subscription with currentPeriodStart, currentPeriodEnd
     * @param oldPlan       plan before change (monthly price used for prorate)
     * @param newPlan       plan after change
     * @return creditVnd (positive = amount to credit/refund), chargeVnd (positive = amount to charge)
     */
    public ProrateResult calculate(SubscriptionEntity subscription, PlanEntity oldPlan, PlanEntity newPlan) {
        if (subscription == null || oldPlan == null || newPlan == null) {
            return ProrateResult.zero();
        }
        Date periodStart = subscription.getCurrentPeriodStart();
        Date periodEnd = subscription.getCurrentPeriodEnd();
        if (periodStart == null || periodEnd == null) {
            return ProrateResult.zero();
        }
        LocalDate start = toLocalDate(periodStart);
        LocalDate end = toLocalDate(periodEnd);
        LocalDate today = LocalDate.now();
        if (!today.isBefore(end.plusDays(1))) {
            return ProrateResult.zero();
        }
        long totalDays = ChronoUnit.DAYS.between(start, end) + 1;
        long daysRemaining = ChronoUnit.DAYS.between(today, end) + 1;
        if (totalDays <= 0 || daysRemaining <= 0) {
            return ProrateResult.zero();
        }
        double ratio = (double) daysRemaining / totalDays;

        int oldMonthly = safePrice(oldPlan.getPriceVndMonthly());
        int newMonthly = safePrice(newPlan.getPriceVndMonthly());
        int diff = newMonthly - oldMonthly;
        if (diff == 0) {
            return ProrateResult.zero();
        }
        int prorated = (int) Math.round(Math.abs(diff) * ratio);
        if (diff > 0) {
            return new ProrateResult(0, prorated);
        } else {
            return new ProrateResult(prorated, 0);
        }
    }

    private static LocalDate toLocalDate(Date date) {
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    private static int safePrice(Integer price) {
        return price == null ? 0 : price;
    }

    @Getter
    public static class ProrateResult {
        private final int creditVnd;
        private final int chargeVnd;

        public ProrateResult(int creditVnd, int chargeVnd) {
            this.creditVnd = Math.max(0, creditVnd);
            this.chargeVnd = Math.max(0, chargeVnd);
        }

        public static ProrateResult zero() {
            return new ProrateResult(0, 0);
        }
    }
}
