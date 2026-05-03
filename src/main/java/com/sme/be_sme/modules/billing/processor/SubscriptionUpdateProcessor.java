package com.sme.be_sme.modules.billing.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.billing.api.request.SubscriptionUpdateRequest;
import com.sme.be_sme.modules.billing.api.response.SubscriptionResponse;
import com.sme.be_sme.modules.billing.infrastructure.mapper.PlanMapper;
import com.sme.be_sme.modules.billing.infrastructure.mapper.SubscriptionMapper;
import com.sme.be_sme.modules.billing.infrastructure.mapper.SubscriptionMapperExt;
import com.sme.be_sme.modules.billing.infrastructure.mapper.SubscriptionPlanHistoryMapper;
import com.sme.be_sme.modules.billing.infrastructure.persistence.entity.PlanEntity;
import com.sme.be_sme.modules.billing.infrastructure.persistence.entity.SubscriptionChangeRequestEntity;
import com.sme.be_sme.modules.billing.infrastructure.persistence.entity.SubscriptionEntity;
import com.sme.be_sme.modules.billing.infrastructure.persistence.entity.SubscriptionPlanHistoryEntity;
import com.sme.be_sme.shared.constant.ErrorCodes;
import com.sme.be_sme.shared.exception.AppException;
import com.sme.be_sme.shared.gateway.core.BaseBizProcessor;
import com.sme.be_sme.shared.gateway.core.BizContext;
import com.sme.be_sme.modules.billing.service.SubscriptionPendingPlanPaymentService;
import com.sme.be_sme.shared.util.UuidGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Date;

@Component
@RequiredArgsConstructor
@Slf4j
public class SubscriptionUpdateProcessor extends BaseBizProcessor<BizContext> {

    private final ObjectMapper objectMapper;
    private final SubscriptionMapper subscriptionMapper;
    private final SubscriptionMapperExt subscriptionMapperExt;
    private final SubscriptionPlanHistoryMapper subscriptionPlanHistoryMapper;
    private final PlanMapper planMapper;
    private final SubscriptionPendingPlanPaymentService pendingPlanPaymentService;

    @Override
    @Transactional
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
                targetPlan = pendingPlanPaymentService.findPlanByCode(context.getTenantId().trim(), request.getPlanCode().trim());
                if (targetPlan == null) {
                    throw AppException.of(ErrorCodes.NOT_FOUND, "plan not found: " + request.getPlanCode());
                }
                targetPlanId = targetPlan.getPlanId();
            }

            String targetBillingCycle = normalizeBillingCycle(request.getBillingCycle(), entity.getBillingCycle());
            boolean cancelAtPeriodEndRequested = isCancelAtPeriodEndRequest(request.getStatus());
            String statusToUpdate = resolveStatusToUpdate(entity, request.getStatus(), cancelAtPeriodEndRequested);

            boolean planChanged = !equalsTrimmed(oldPlanId, targetPlanId);
            boolean cycleChanged = !equalsTrimmed(oldBillingCycle, targetBillingCycle);

            int oldPrice = pendingPlanPaymentService.calculatePlanChangeCharge(oldPlan, targetBillingCycle);
            int newPrice = pendingPlanPaymentService.calculatePlanChangeCharge(targetPlan, targetBillingCycle);
            int chargeAmount = 0;
            if (planChanged || cycleChanged) {
                pendingPlanPaymentService.validateDowngradeActiveOnboardingLimit(context.getTenantId().trim(), oldPrice, newPrice, targetPlan);
                chargeAmount = pendingPlanPaymentService.calculatePlanChangeCharge(targetPlan, targetBillingCycle);
            }

            Date updatedAt = new Date();
            if (cancelAtPeriodEndRequested) {
                applyCancelAtPeriodEnd(entity, updatedAt);
                SubscriptionEntity latest = subscriptionMapper.selectByPrimaryKey(subscriptionId);
                if (latest != null) {
                    entity = latest;
                }
                return buildResponse(entity);
            }

            if ((planChanged || cycleChanged) && chargeAmount > 0) {
                SubscriptionChangeRequestEntity pending = pendingPlanPaymentService.getOrCreatePendingChangeRequest(
                        entity,
                        oldPlanId,
                        targetPlanId,
                        targetBillingCycle,
                        chargeAmount,
                        updatedAt,
                        context.getOperatorId()
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

            SubscriptionResponse response = buildResponse(entity);

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

    private SubscriptionResponse buildResponse(SubscriptionEntity entity) {
        SubscriptionResponse response = new SubscriptionResponse();
        response.setSubscriptionId(entity.getSubscriptionId());
        response.setStatus(entity.getStatus());
        response.setPlanCode(resolvePlanCode(entity.getPlanId()));
        response.setBillingCycle(entity.getBillingCycle());
        response.setCurrentPeriodStart(entity.getCurrentPeriodStart());
        response.setCurrentPeriodEnd(entity.getCurrentPeriodEnd());
        response.setAutoRenew(entity.getAutoRenew());
        response.setPaymentRequired(Boolean.FALSE);
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

    private static boolean isCancelAtPeriodEndRequest(String status) {
        if (!StringUtils.hasText(status)) {
            return false;
        }
        String normalized = status.trim().toUpperCase();
        return "CANCEL".equals(normalized)
                || "CANCELED".equals(normalized)
                || "CANCELLED".equals(normalized)
                || "CANCEL_AT_PERIOD_END".equals(normalized);
    }

    private static String resolveStatusToUpdate(SubscriptionEntity entity, String requestedStatus, boolean cancelAtPeriodEndRequested) {
        if (cancelAtPeriodEndRequested) {
            return entity.getStatus() != null ? entity.getStatus() : "ACTIVE";
        }
        if (StringUtils.hasText(requestedStatus)) {
            return requestedStatus.trim();
        }
        return entity.getStatus() != null ? entity.getStatus() : "ACTIVE";
    }

    private void applyCancelAtPeriodEnd(SubscriptionEntity subscription, Date now) {
        int updated = subscriptionMapperExt.updateAutoRenewAndUpdatedAt(subscription.getSubscriptionId(), Boolean.FALSE, now);
        if (updated == 0) {
            SubscriptionEntity latest = subscriptionMapper.selectByPrimaryKey(subscription.getSubscriptionId());
            if (latest == null) {
                throw AppException.of(ErrorCodes.NOT_FOUND, "subscription not found");
            }
            if (!Boolean.FALSE.equals(latest.getAutoRenew())) {
                throw AppException.of(ErrorCodes.INTERNAL_ERROR, "failed to schedule subscription cancel at period end");
            }
            return;
        }
        if (updated != 1) {
            throw AppException.of(ErrorCodes.INTERNAL_ERROR, "failed to schedule subscription cancel at period end");
        }
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
}
