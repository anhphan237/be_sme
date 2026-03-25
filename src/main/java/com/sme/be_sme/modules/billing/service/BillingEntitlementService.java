package com.sme.be_sme.modules.billing.service;

import com.sme.be_sme.modules.billing.infrastructure.mapper.InvoiceMapperExt;
import com.sme.be_sme.modules.billing.infrastructure.mapper.PlanMapper;
import com.sme.be_sme.modules.billing.infrastructure.mapper.SubscriptionMapper;
import com.sme.be_sme.modules.billing.infrastructure.persistence.entity.InvoiceEntity;
import com.sme.be_sme.modules.billing.infrastructure.persistence.entity.PlanEntity;
import com.sme.be_sme.modules.billing.infrastructure.persistence.entity.SubscriptionEntity;
import com.sme.be_sme.shared.constant.ErrorCodes;
import com.sme.be_sme.shared.exception.AppException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Enforces that a company has settled billing before operational features (e.g. onboarding)
 * when on a paid plan: no overdue ISSUED invoice and no ISSUED invoice for the current subscription period.
 */
@Service
@RequiredArgsConstructor
public class BillingEntitlementService {

    private final SubscriptionMapper subscriptionMapper;
    private final PlanMapper planMapper;
    private final InvoiceMapperExt invoiceMapperExt;

    @Value("${app.billing.require-paid-for-operations:true}")
    private boolean requirePaidForOperations;

    /**
     * Throws if the tenant must pay an invoice before using paid features.
     */
    public void assertPaidForOperationalAccess(String companyId) {
        if (!requirePaidForOperations || !StringUtils.hasText(companyId)) {
            return;
        }
        String cid = companyId.trim();
        SubscriptionEntity sub = resolveActiveSubscription(cid);
        if (sub == null) {
            return;
        }
        PlanEntity plan = StringUtils.hasText(sub.getPlanId()) ? planMapper.selectByPrimaryKey(sub.getPlanId()) : null;
        if (isNoPaymentRequired(plan)) {
            return;
        }
        InvoiceEntity open = invoiceMapperExt.selectLatestIssuedBySubscriptionId(cid, sub.getSubscriptionId());
        if (open == null) {
            return;
        }
        Date now = new Date();
        if (blocksAccess(open, sub, now)) {
            throw AppException.of(ErrorCodes.PAYMENT_REQUIRED, "Payment required: settle the current invoice to continue");
        }
    }

    private SubscriptionEntity resolveActiveSubscription(String companyId) {
        List<SubscriptionEntity> list = subscriptionMapper.selectAll();
        if (list == null) {
            return null;
        }
        return list.stream()
                .filter(Objects::nonNull)
                .filter(s -> companyId.equals(s.getCompanyId()))
                .filter(s -> "ACTIVE".equalsIgnoreCase(trimLower(s.getStatus())))
                .max(Comparator.comparing(SubscriptionEntity::getUpdatedAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .orElse(null);
    }

    private static boolean isNoPaymentRequired(PlanEntity plan) {
        if (plan == null) {
            return false;
        }
        if ("FREE".equalsIgnoreCase(trimLower(plan.getCode()))) {
            return true;
        }
        int monthly = plan.getPriceVndMonthly() == null ? 0 : plan.getPriceVndMonthly();
        int yearly = plan.getPriceVndYearly() == null ? 0 : plan.getPriceVndYearly();
        return monthly <= 0 && yearly <= 0;
    }

    /**
     * Open invoice blocks if amount &gt; 0 and (overdue OR issued in current subscription period).
     */
    private static boolean blocksAccess(InvoiceEntity issued, SubscriptionEntity sub, Date now) {
        int amt = issued.getAmountTotal() == null ? 0 : issued.getAmountTotal();
        if (amt <= 0) {
            return false;
        }
        Date due = issued.getDueAt();
        if (due != null && due.before(now)) {
            return true;
        }
        Date start = sub.getCurrentPeriodStart();
        Date end = sub.getCurrentPeriodEnd();
        Date issue = issued.getIssuedAt();
        if (start != null && end != null && issue != null) {
            return !issue.before(start) && !issue.after(end);
        }
        return true;
    }

    private static String trimLower(String value) {
        return value == null ? null : value.trim().toLowerCase(Locale.ROOT);
    }
}
