package com.sme.be_sme.modules.billing.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.billing.api.request.SubscriptionUpdateRequest;
import com.sme.be_sme.modules.billing.api.response.SubscriptionResponse;
import com.sme.be_sme.modules.billing.enums.InvoiceStatus;
import com.sme.be_sme.modules.billing.enums.SubscriptionChangeRequestStatus;
import com.sme.be_sme.modules.billing.infrastructure.mapper.InvoiceMapper;
import com.sme.be_sme.modules.billing.infrastructure.mapper.PlanMapper;
import com.sme.be_sme.modules.billing.infrastructure.mapper.SubscriptionChangeRequestMapper;
import com.sme.be_sme.modules.billing.infrastructure.mapper.SubscriptionMapper;
import com.sme.be_sme.modules.billing.infrastructure.mapper.SubscriptionMapperExt;
import com.sme.be_sme.modules.billing.infrastructure.mapper.SubscriptionPlanHistoryMapper;
import com.sme.be_sme.modules.billing.infrastructure.persistence.entity.InvoiceEntity;
import com.sme.be_sme.modules.billing.infrastructure.persistence.entity.PlanEntity;
import com.sme.be_sme.modules.billing.infrastructure.persistence.entity.SubscriptionChangeRequestEntity;
import com.sme.be_sme.modules.billing.infrastructure.persistence.entity.SubscriptionEntity;
import com.sme.be_sme.modules.billing.infrastructure.persistence.entity.SubscriptionPlanHistoryEntity;
import com.sme.be_sme.modules.billing.service.ProrateService;
import com.sme.be_sme.shared.constant.ErrorCodes;
import com.sme.be_sme.shared.exception.AppException;
import com.sme.be_sme.shared.gateway.core.BaseBizProcessor;
import com.sme.be_sme.shared.gateway.core.BizContext;
import com.sme.be_sme.shared.util.UuidGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Objects;

@Component
@RequiredArgsConstructor
@Slf4j
public class SubscriptionUpdateProcessor extends BaseBizProcessor<BizContext> {

    private final ObjectMapper objectMapper;
    private final SubscriptionMapper subscriptionMapper;
    private final SubscriptionMapperExt subscriptionMapperExt;
    private final SubscriptionPlanHistoryMapper subscriptionPlanHistoryMapper;
    private final SubscriptionChangeRequestMapper subscriptionChangeRequestMapper;
    private final InvoiceMapper invoiceMapper;
    private final PlanMapper planMapper;
    private final ProrateService prorateService;

    @Override
    protected Object doProcess(BizContext context, JsonNode payload) {
        try {
            SubscriptionUpdateRequest request = objectMapper.convertValue(payload, SubscriptionUpdateRequest.class);
            validate(context, request);

            String subscriptionId = request.getSubscriptionId().trim();
            SubscriptionEntity entity = subscriptionMapper.selectByPrimaryKey(subscriptionId);
            if (entity == null || !context.getTenantId().trim().equals(entity.getCompanyId())) {
                throw AppException.of(ErrorCodes.NOT_FOUND, "subscription not found");
            }
            String oldPlanId = entity.getPlanId();
            String oldBillingCycle = entity.getBillingCycle();

            PlanEntity oldPlan = null;
            if (StringUtils.hasText(entity.getPlanId())) {
                oldPlan = planMapper.selectByPrimaryKey(entity.getPlanId());
            }
            String oldPlanCode = oldPlan == null ? null : oldPlan.getCode();

            String targetPlanId = entity.getPlanId();
            PlanEntity targetPlan = oldPlan;
            if (StringUtils.hasText(request.getPlanCode())) {
                targetPlan = findPlanByCode(context.getTenantId().trim(), request.getPlanCode().trim());
                if (targetPlan == null) {
                    throw AppException.of(ErrorCodes.NOT_FOUND, "plan not found: " + request.getPlanCode());
                }
                targetPlanId = targetPlan.getPlanId();
            }

            String targetBillingCycle = normalizeBillingCycle(request.getBillingCycle(), entity.getBillingCycle());
            String statusToUpdate = StringUtils.hasText(request.getStatus())
                    ? request.getStatus().trim()
                    : (entity.getStatus() != null ? entity.getStatus() : "ACTIVE");

            boolean planChanged = !equalsTrimmed(oldPlanId, targetPlanId);
            boolean cycleChanged = !equalsTrimmed(oldBillingCycle, targetBillingCycle);

            int chargeAmount = 0;
            if (planChanged || cycleChanged) {
                chargeAmount = estimatePlanChangeCharge(entity, oldPlan, targetPlan, targetBillingCycle);
            }

            Date updatedAt = new Date();
            if ((planChanged || cycleChanged) && chargeAmount > 0) {
                ensureNoOpenPendingChange(entity);
                InvoiceEntity invoice = createPlanChangeInvoice(entity, chargeAmount, updatedAt);
                SubscriptionChangeRequestEntity pending = createPendingChangeRequest(
                        context, entity, oldPlanId, targetPlanId, targetBillingCycle, invoice.getInvoiceId(), updatedAt
                );
                return buildPendingResponse(entity, targetPlan, targetBillingCycle, pending, chargeAmount);
            }

            entity.setPlanId(targetPlanId);
            entity.setBillingCycle(targetBillingCycle);
            entity.setStatus(statusToUpdate);
            entity.setUpdatedAt(updatedAt);

            int updated = subscriptionMapperExt.updatePlanAndStatus(
                    entity.getSubscriptionId(),
                    entity.getPlanId(),
                    entity.getBillingCycle(),
                    statusToUpdate,
                    updatedAt);
            if (updated == 0) {
                // Some databases report 0 affected rows when values are unchanged.
                SubscriptionEntity latest = subscriptionMapper.selectByPrimaryKey(subscriptionId);
                if (latest == null) {
                    throw AppException.of(ErrorCodes.NOT_FOUND, "subscription not found");
                }
                if (!matchesDesiredState(latest, entity)) {
                    // Avoid failing user flow on uncertain affected-row reporting.
                    log.warn("SubscriptionUpdateProcessor: update returned 0 and database state differs for subscriptionId={}, keep latest state", subscriptionId);
                }
                log.info("SubscriptionUpdateProcessor: no-op update for subscription {}", subscriptionId);
                entity = latest;
            } else if (updated == 1) {
                SubscriptionEntity latest = subscriptionMapper.selectByPrimaryKey(subscriptionId);
                if (latest != null) {
                    entity = latest;
                }
            } else {
                log.error("SubscriptionUpdateProcessor: update failed (returned {}) for subscriptionId={}", updated, subscriptionId);
                throw AppException.of(ErrorCodes.INTERNAL_ERROR, "update subscription failed");
            }

            if (planChanged) {
                try {
                    savePlanChangeHistory(entity, oldPlanId, context.getOperatorId(), updatedAt);
                } catch (Exception historyEx) {
                    // Keep plan change successful even if history table/migration is unavailable.
                    log.warn(
                            "SubscriptionUpdateProcessor: history write failed tenantId={} subscriptionId={} oldPlanId={} newPlanId={} - {}",
                            context.getTenantId(),
                            entity.getSubscriptionId(),
                            oldPlanId,
                            entity.getPlanId(),
                            historyEx.getMessage(),
                            historyEx);
                }
            }

            SubscriptionResponse response = buildResponse(entity, oldPlan, targetPlan);

            String newPlanCode = resolvePlanCode(entity.getPlanId());
            log.info(
                    "SubscriptionUpdateProcessor: plan change tenantId={} operatorId={} subscriptionId={} oldPlan={} newPlan={} billingCycle={} status={}",
                    context.getTenantId(),
                    context.getOperatorId(),
                    subscriptionId,
                    oldPlanCode,
                    newPlanCode,
                    entity.getBillingCycle(),
                    entity.getStatus());
            return response;
        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            log.error("SubscriptionUpdateProcessor: unexpected error - {}", e.getMessage(), e);
            String reason = e.getMessage() != null && !e.getMessage().isBlank()
                    ? e.getMessage()
                    : e.getClass().getSimpleName();
            throw AppException.of(ErrorCodes.INTERNAL_ERROR,
                    "Failed to update subscription: " + reason);
        }
    }

    private SubscriptionResponse buildResponse(SubscriptionEntity entity, PlanEntity oldPlan, PlanEntity newPlan) {
        SubscriptionResponse response = new SubscriptionResponse();
        response.setSubscriptionId(entity.getSubscriptionId());
        response.setStatus(entity.getStatus());
        response.setPlanCode(resolvePlanCode(entity.getPlanId()));
        response.setBillingCycle(entity.getBillingCycle());
        response.setCurrentPeriodStart(entity.getCurrentPeriodStart());
        response.setCurrentPeriodEnd(entity.getCurrentPeriodEnd());
        response.setAutoRenew(entity.getAutoRenew());
        response.setPaymentRequired(Boolean.FALSE);

        if (oldPlan != null && newPlan != null && !Objects.equals(oldPlan.getPlanId(), newPlan.getPlanId())) {
            ProrateService.ProrateResult prorate = prorateService.calculate(entity, oldPlan, newPlan);
            response.setProrateCreditVnd(prorate.getCreditVnd() > 0 ? prorate.getCreditVnd() : null);
            response.setProrateChargeVnd(prorate.getChargeVnd() > 0 ? prorate.getChargeVnd() : null);
        }
        return response;
    }

    private SubscriptionResponse buildPendingResponse(SubscriptionEntity current,
                                                      PlanEntity targetPlan,
                                                      String targetBillingCycle,
                                                      SubscriptionChangeRequestEntity pending,
                                                      int chargeAmount) {
        SubscriptionResponse response = new SubscriptionResponse();
        response.setSubscriptionId(current.getSubscriptionId());
        response.setStatus(current.getStatus());
        response.setPlanCode(resolvePlanCode(current.getPlanId()));
        response.setBillingCycle(current.getBillingCycle());
        response.setCurrentPeriodStart(current.getCurrentPeriodStart());
        response.setCurrentPeriodEnd(current.getCurrentPeriodEnd());
        response.setAutoRenew(current.getAutoRenew());
        response.setPaymentRequired(Boolean.TRUE);
        response.setPendingChangeId(pending.getSubscriptionChangeRequestId());
        response.setPaymentInvoiceId(pending.getInvoiceId());
        response.setPendingPlanCode(targetPlan == null ? null : targetPlan.getCode());
        response.setPendingBillingCycle(targetBillingCycle);
        response.setProrateChargeVnd(chargeAmount > 0 ? chargeAmount : null);
        return response;
    }

    private static void validate(BizContext context, SubscriptionUpdateRequest request) {
        if (context == null || !StringUtils.hasText(context.getTenantId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "tenantId is required");
        }
        if (request == null) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "payload is required");
        }
        if (!StringUtils.hasText(request.getSubscriptionId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "subscriptionId is required");
        }
    }

    private String normalizeBillingCycle(String requestedCycle, String currentCycle) {
        if (!StringUtils.hasText(requestedCycle)) {
            return StringUtils.hasText(currentCycle) ? currentCycle.trim().toUpperCase() : "MONTHLY";
        }
        String cycle = requestedCycle.trim().toUpperCase();
        if (!"MONTHLY".equals(cycle) && !"YEARLY".equals(cycle)) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "billingCycle must be MONTHLY or YEARLY");
        }
        return cycle;
    }

    private int estimatePlanChangeCharge(SubscriptionEntity current,
                                         PlanEntity oldPlan,
                                         PlanEntity newPlan,
                                         String targetBillingCycle) {
        if (newPlan == null) return 0;
        int oldPrice = resolvePlanPrice(oldPlan, targetBillingCycle);
        int newPrice = resolvePlanPrice(newPlan, targetBillingCycle);
        if (newPrice <= oldPrice) {
            return 0;
        }
        if (oldPlan == null) {
            return newPrice;
        }

        SubscriptionEntity calcBase = new SubscriptionEntity();
        calcBase.setBillingCycle(targetBillingCycle);
        calcBase.setCurrentPeriodStart(current.getCurrentPeriodStart());
        calcBase.setCurrentPeriodEnd(current.getCurrentPeriodEnd());

        ProrateService.ProrateResult prorate = prorateService.calculate(calcBase, oldPlan, newPlan);
        if (prorate.getChargeVnd() > 0) {
            return prorate.getChargeVnd();
        }
        if (oldPrice == 0) {
            return newPrice;
        }
        return Math.max(0, newPrice - oldPrice);
    }

    private int resolvePlanPrice(PlanEntity plan, String billingCycle) {
        if (plan == null) return 0;
        if ("YEARLY".equalsIgnoreCase(billingCycle)) {
            return plan.getPriceVndYearly() == null ? 0 : Math.max(0, plan.getPriceVndYearly());
        }
        return plan.getPriceVndMonthly() == null ? 0 : Math.max(0, plan.getPriceVndMonthly());
    }

    private void ensureNoOpenPendingChange(SubscriptionEntity subscription) {
        SubscriptionChangeRequestEntity existing = subscriptionChangeRequestMapper.selectOpenBySubscriptionId(
                subscription.getCompanyId(), subscription.getSubscriptionId()
        );
        if (existing != null) {
            throw AppException.of(
                    ErrorCodes.BAD_REQUEST,
                    "A subscription change is already pending payment (invoiceId=" + existing.getInvoiceId() + ")"
            );
        }
    }

    private InvoiceEntity createPlanChangeInvoice(SubscriptionEntity current, int chargeAmount, Date now) {
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
        invoice.setDueAt(addDays(now, 7));
        invoice.setCreatedAt(now);
        int inserted = invoiceMapper.insert(invoice);
        if (inserted != 1) {
            throw AppException.of(ErrorCodes.INTERNAL_ERROR, "failed to create payment invoice for plan change");
        }
        return invoice;
    }

    private SubscriptionChangeRequestEntity createPendingChangeRequest(BizContext context,
                                                                       SubscriptionEntity current,
                                                                       String oldPlanId,
                                                                       String newPlanId,
                                                                       String billingCycle,
                                                                       String invoiceId,
                                                                       Date now) {
        SubscriptionChangeRequestEntity req = new SubscriptionChangeRequestEntity();
        req.setSubscriptionChangeRequestId(UuidGenerator.generate());
        req.setCompanyId(current.getCompanyId());
        req.setSubscriptionId(current.getSubscriptionId());
        req.setOldPlanId(oldPlanId);
        req.setNewPlanId(newPlanId);
        req.setBillingCycle(billingCycle);
        req.setInvoiceId(invoiceId);
        req.setStatus(SubscriptionChangeRequestStatus.PENDING_PAYMENT.getCode());
        req.setRequestedBy(context.getOperatorId());
        req.setRequestedAt(now);
        req.setCreatedAt(now);
        req.setUpdatedAt(now);
        int inserted = subscriptionChangeRequestMapper.insert(req);
        if (inserted != 1) {
            throw AppException.of(ErrorCodes.INTERNAL_ERROR, "failed to create pending subscription change");
        }
        return req;
    }

    private PlanEntity findPlanByCode(String companyId, String planCode) {
        return planMapper.selectAll().stream()
                .filter(plan -> plan != null)
                .filter(plan -> planCode.equalsIgnoreCase(plan.getCode()))
                .filter(plan -> companyId.equals(plan.getCompanyId()) || plan.getCompanyId() == null)
                .findFirst()
                .orElse(null);
    }

    private String resolvePlanCode(String planId) {
        if (!StringUtils.hasText(planId)) {
            return null;
        }
        PlanEntity plan = planMapper.selectByPrimaryKey(planId);
        return plan == null ? null : plan.getCode();
    }

    private static boolean matchesDesiredState(SubscriptionEntity actual, SubscriptionEntity desired) {
        if (actual == null || desired == null) {
            return false;
        }
        return equalsTrimmed(actual.getPlanId(), desired.getPlanId())
                && equalsTrimmed(actual.getBillingCycle(), desired.getBillingCycle())
                && equalsTrimmed(actual.getStatus(), desired.getStatus());
    }

    private static boolean equalsTrimmed(String left, String right) {
        String l = left == null ? null : left.trim();
        String r = right == null ? null : right.trim();
        if (l == null) {
            return r == null;
        }
        return l.equals(r);
    }

    private void savePlanChangeHistory(SubscriptionEntity entity, String oldPlanId, String changedBy, Date changeTime) {
        subscriptionPlanHistoryMapper.closeOpenBySubscription(entity.getCompanyId(), entity.getSubscriptionId(), changeTime);

        SubscriptionPlanHistoryEntity history = new SubscriptionPlanHistoryEntity();
        history.setSubscriptionPlanHistoryId(UuidGenerator.generate());
        history.setCompanyId(entity.getCompanyId());
        history.setSubscriptionId(entity.getSubscriptionId());
        history.setOldPlanId(oldPlanId);
        history.setNewPlanId(entity.getPlanId());
        history.setBillingCycle(entity.getBillingCycle());
        history.setChangedBy(changedBy);
        history.setChangedAt(changeTime);
        history.setEffectiveFrom(changeTime);
        history.setEffectiveTo(null);
        history.setCreatedAt(changeTime);
        subscriptionPlanHistoryMapper.insert(history);
    }

    private static String buildInvoiceNo(String invoiceId) {
        String suffix = invoiceId.length() > 6 ? invoiceId.substring(invoiceId.length() - 6) : invoiceId;
        String today = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        return "INV-" + today + "-" + suffix;
    }

    private static Date addDays(Date start, int days) {
        LocalDate date = start.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        return Date.from(date.plusDays(days).atStartOfDay(ZoneId.systemDefault()).toInstant());
    }
}
