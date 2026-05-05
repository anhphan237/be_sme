package com.sme.be_sme.modules.billing.service;

import com.sme.be_sme.modules.billing.enums.InvoiceStatus;
import com.sme.be_sme.modules.billing.enums.SubscriptionChangeRequestStatus;
import com.sme.be_sme.modules.billing.infrastructure.mapper.InvoiceMapper;
import com.sme.be_sme.modules.billing.infrastructure.mapper.OnboardingUsageMapper;
import com.sme.be_sme.modules.billing.infrastructure.mapper.PlanMapper;
import com.sme.be_sme.modules.billing.infrastructure.mapper.SubscriptionChangeRequestMapper;
import com.sme.be_sme.modules.billing.infrastructure.mapper.SubscriptionMapper;
import com.sme.be_sme.modules.billing.infrastructure.mapper.SubscriptionMapperExt;
import com.sme.be_sme.modules.billing.infrastructure.persistence.entity.InvoiceEntity;
import com.sme.be_sme.modules.billing.infrastructure.persistence.entity.PlanEntity;
import com.sme.be_sme.modules.billing.infrastructure.persistence.entity.SubscriptionChangeRequestEntity;
import com.sme.be_sme.modules.billing.infrastructure.persistence.entity.SubscriptionEntity;
import com.sme.be_sme.shared.constant.ErrorCodes;
import com.sme.be_sme.shared.exception.AppException;
import com.sme.be_sme.shared.util.UuidGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;

/**
 * Pending invoice + subscription_change_requests row for upgrades that require payment.
 * Shared by {@link com.sme.be_sme.modules.billing.processor.SubscriptionUpdateProcessor} and company registration.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionPendingPlanPaymentService {

    private final SubscriptionMapper subscriptionMapper;
    private final SubscriptionMapperExt subscriptionMapperExt;
    private final SubscriptionChangeRequestMapper subscriptionChangeRequestMapper;
    private final InvoiceMapper invoiceMapper;
    private final PlanMapper planMapper;
    private final OnboardingUsageMapper onboardingUsageMapper;

    /**
     * After registration with a paid intent: subscription stays on FREE until payment; creates pending upgrade + ISSUED invoice.
     */
    public void enqueuePaidPlanIntentAfterRegistration(String companyId,
                                                       String subscriptionId,
                                                       String operatorUserId,
                                                       String desiredPlanCode,
                                                       String requestedBillingCycle) {
        if (!StringUtils.hasText(desiredPlanCode) || "FREE".equalsIgnoreCase(desiredPlanCode.trim())) {
            return;
        }
        if (!StringUtils.hasText(subscriptionId) || !StringUtils.hasText(companyId)) {
            return;
        }

        SubscriptionEntity sub = subscriptionMapper.selectByPrimaryKey(subscriptionId.trim());
        if (sub == null || !companyId.trim().equals(sub.getCompanyId())) {
            throw AppException.of(ErrorCodes.NOT_FOUND, "subscription not found");
        }

        PlanEntity targetPlan = findPlanByCode(companyId.trim(), desiredPlanCode.trim());
        if (targetPlan == null) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "Plan này không hợp lệ");
        }

        String oldPlanId = sub.getPlanId();
        String targetPlanId = targetPlan.getPlanId();
        if (equalsTrimmed(oldPlanId, targetPlanId)) {
            return;
        }

        PlanEntity oldPlan = StringUtils.hasText(oldPlanId) ? planMapper.selectByPrimaryKey(oldPlanId) : null;
        String targetBillingCycle = normalizeBillingCycle(requestedBillingCycle, sub.getBillingCycle());

        boolean planChanged = !equalsTrimmed(oldPlanId, targetPlanId);
        boolean cycleChanged = !equalsTrimmed(sub.getBillingCycle(), targetBillingCycle);
        int oldPrice = resolvePlanPrice(oldPlan, targetBillingCycle);
        int newPrice = resolvePlanPrice(targetPlan, targetBillingCycle);

        Date now = new Date();
        int chargeAmount = 0;
        if (planChanged || cycleChanged) {
            validateDowngradeActiveOnboardingLimit(companyId.trim(), oldPrice, newPrice, targetPlan);
            chargeAmount = calculatePlanChangeCharge(targetPlan, targetBillingCycle);
        }

        if (!planChanged && !cycleChanged) {
            return;
        }

        if (chargeAmount > 0) {
            getOrCreatePendingChangeRequest(
                    sub,
                    oldPlanId,
                    targetPlanId,
                    targetBillingCycle,
                    chargeAmount,
                    now,
                    operatorUserId
            );
            return;
        }

        int updated = subscriptionMapperExt.updatePlanAndStatus(
                sub.getSubscriptionId(),
                targetPlanId,
                targetBillingCycle,
                sub.getStatus() != null ? sub.getStatus() : "ACTIVE",
                now);
        if (updated == 0) {
            SubscriptionEntity latest = subscriptionMapper.selectByPrimaryKey(sub.getSubscriptionId());
            if (latest == null) {
                throw AppException.of(ErrorCodes.NOT_FOUND, "subscription not found");
            }
        }
        log.info(
                "Registration paid plan ({}) applied immediately without payment (charge 0)",
                desiredPlanCode.trim());
    }

    /**
     * For subscription gateway updates when plan/cycle changes with a charge.
     */
    public SubscriptionChangeRequestEntity getOrCreatePendingChangeRequest(SubscriptionEntity current,
                                                                          String oldPlanId,
                                                                          String newPlanId,
                                                                          String billingCycle,
                                                                          int chargeAmount,
                                                                          Date now,
                                                                          String requestedByUserId) {
        SubscriptionChangeRequestEntity existing = subscriptionChangeRequestMapper.selectOpenBySubscriptionId(
                current.getCompanyId(),
                current.getSubscriptionId()
        );
        if (existing != null) {
            if (isSamePendingTarget(existing, newPlanId, billingCycle)) {
                return existing;
            }
            throw AppException.of(
                    ErrorCodes.BAD_REQUEST,
                    "A different subscription change is already pending payment (invoiceId=" + existing.getInvoiceId() + ")"
            );
        }

        InvoiceEntity invoice = createPlanChangeInvoice(current, chargeAmount, billingCycle, now);
        return createPendingChangeRequest(
                current,
                oldPlanId,
                newPlanId,
                billingCycle,
                invoice.getInvoiceId(),
                now,
                requestedByUserId
        );
    }

    public PlanEntity findPlanByCode(String companyId, String planCode) {
        return planMapper.selectAll().stream()
                .filter(plan -> plan != null)
                .filter(plan -> planCode.equalsIgnoreCase(plan.getCode()))
                .filter(plan -> companyId.equals(plan.getCompanyId()) || plan.getCompanyId() == null)
                .findFirst()
                .orElse(null);
    }

    public int calculatePlanChangeCharge(PlanEntity newPlan, String targetBillingCycle) {
        if (newPlan == null) return 0;
        return resolvePlanPrice(newPlan, targetBillingCycle);
    }

    public void validateDowngradeActiveOnboardingLimit(String companyId,
                                                       int oldPrice,
                                                       int newPrice,
                                                       PlanEntity targetPlan) {
        if (newPrice >= oldPrice) {
            return;
        }
        if (targetPlan == null || targetPlan.getEmployeeLimitPerMonth() == null) {
            return;
        }
        int targetLimit = Math.max(0, targetPlan.getEmployeeLimitPerMonth());
        int activeCount = onboardingUsageMapper.countActiveOnboardingInstancesByCompanyId(companyId);
        if (activeCount > targetLimit) {
            throw AppException.of(
                    ErrorCodes.BAD_REQUEST,
                    "Cannot downgrade: ACTIVE onboarding count (" + activeCount
                            + ") exceeds target plan limit (" + targetLimit + ")"
            );
        }
    }

    private InvoiceEntity createPlanChangeInvoice(SubscriptionEntity current,
                                                  int chargeAmount,
                                                  String billingCycle,
                                                  Date now) {
        InvoiceEntity invoice = new InvoiceEntity();
        String invoiceId = UuidGenerator.generate();
        invoice.setInvoiceId(invoiceId);
        invoice.setCompanyId(current.getCompanyId());
        invoice.setSubscriptionId(current.getSubscriptionId());
        invoice.setInvoiceNo(buildInvoiceNo(invoiceId));
        invoice.setAmountTotal(chargeAmount);
        invoice.setCurrency("VND");
        invoice.setStatus(InvoiceStatus.ISSUED.getCode());
        invoice.setIssuedAt(now);
        Date dueAt = resolveDueAt(now, billingCycle);
        invoice.setDueAt(dueAt);
        invoice.setExpiredAt(dueAt);
        invoice.setCreatedAt(now);
        int inserted = invoiceMapper.insert(invoice);
        if (inserted != 1) {
            throw AppException.of(ErrorCodes.INTERNAL_ERROR, "failed to create payment invoice for plan change");
        }
        return invoice;
    }

    private SubscriptionChangeRequestEntity createPendingChangeRequest(SubscriptionEntity current,
                                                                       String oldPlanId,
                                                                       String newPlanId,
                                                                       String billingCycle,
                                                                       String invoiceId,
                                                                       Date now,
                                                                       String requestedByUserId) {
        SubscriptionChangeRequestEntity req = new SubscriptionChangeRequestEntity();
        req.setSubscriptionChangeRequestId(UuidGenerator.generate());
        req.setCompanyId(current.getCompanyId());
        req.setSubscriptionId(current.getSubscriptionId());
        req.setOldPlanId(oldPlanId);
        req.setNewPlanId(newPlanId);
        req.setBillingCycle(billingCycle);
        req.setInvoiceId(invoiceId);
        req.setStatus(SubscriptionChangeRequestStatus.PENDING_PAYMENT.getCode());
        req.setRequestedBy(requestedByUserId);
        req.setRequestedAt(now);
        req.setCreatedAt(now);
        req.setUpdatedAt(now);
        int inserted = subscriptionChangeRequestMapper.insert(req);
        if (inserted != 1) {
            throw AppException.of(ErrorCodes.INTERNAL_ERROR, "failed to create pending subscription change");
        }
        return req;
    }

    private static String normalizeBillingCycle(String requestedCycle, String currentCycle) {
        if (!StringUtils.hasText(requestedCycle)) {
            return StringUtils.hasText(currentCycle) ? currentCycle.trim().toUpperCase() : "MONTHLY";
        }
        String cycle = requestedCycle.trim().toUpperCase();
        if (!"MONTHLY".equals(cycle) && !"YEARLY".equals(cycle)) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "billingCycle must be MONTHLY or YEARLY");
        }
        return cycle;
    }

    private int resolvePlanPrice(PlanEntity plan, String billingCycle) {
        if (plan == null) return 0;
        if ("YEARLY".equalsIgnoreCase(billingCycle)) {
            return plan.getPriceVndYearly() == null ? 0 : Math.max(0, plan.getPriceVndYearly());
        }
        return plan.getPriceVndMonthly() == null ? 0 : Math.max(0, plan.getPriceVndMonthly());
    }

    private static boolean equalsTrimmed(String left, String right) {
        String l = left == null ? null : left.trim();
        String r = right == null ? null : right.trim();
        if (l == null) {
            return r == null;
        }
        return l.equals(r);
    }

    private static boolean isSamePendingTarget(SubscriptionChangeRequestEntity existing, String newPlanId, String billingCycle) {
        return equalsTrimmed(existing.getNewPlanId(), newPlanId)
                && equalsTrimmed(existing.getBillingCycle(), billingCycle);
    }

    private static String buildInvoiceNo(String invoiceId) {
        String suffix = invoiceId.length() > 6 ? invoiceId.substring(invoiceId.length() - 6) : invoiceId;
        String today = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        return "INV-" + today + "-" + suffix;
    }

    private static Date resolveDueAt(Date issuedAt, String billingCycle) {
        ZonedDateTime issuedTime = issuedAt.toInstant().atZone(ZoneId.systemDefault());
        ZonedDateTime dueTime = "YEARLY".equalsIgnoreCase(billingCycle)
                ? issuedTime.plusYears(1)
                : issuedTime.plusMonths(1);
        return Date.from(dueTime.toInstant());
    }
}
