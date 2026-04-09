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

            PlanEntity oldPlan = null;
            if (StringUtils.hasText(entity.getPlanId())) {
                oldPlan = planMapper.selectByPrimaryKey(entity.getPlanId());
            }
            String oldPlanCode = oldPlan == null ? null : oldPlan.getCode();

            if (StringUtils.hasText(request.getPlanCode())) {
                PlanEntity newPlan = findPlanByCode(context.getTenantId().trim(), request.getPlanCode().trim());
                if (newPlan == null) {
                    throw AppException.of(ErrorCodes.NOT_FOUND, "plan not found: " + request.getPlanCode());
                }
                entity.setPlanId(newPlan.getPlanId());
            }

            if (StringUtils.hasText(request.getBillingCycle())) {
                String cycle = request.getBillingCycle().trim().toUpperCase();
                if (!"MONTHLY".equals(cycle) && !"YEARLY".equals(cycle)) {
                    throw AppException.of(ErrorCodes.BAD_REQUEST, "billingCycle must be MONTHLY or YEARLY");
                }
                entity.setBillingCycle(cycle);
            }

            if (StringUtils.hasText(request.getStatus())) {
                entity.setStatus(request.getStatus().trim());
            }
            String statusToUpdate = entity.getStatus() != null ? entity.getStatus() : "ACTIVE";
            entity.setStatus(statusToUpdate);
            Date updatedAt = new Date();
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
                    log.error("SubscriptionUpdateProcessor: update returned 0 and database state differs for subscriptionId={}", subscriptionId);
                    throw AppException.of(ErrorCodes.INTERNAL_ERROR, "update subscription failed");
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

            if (!equalsTrimmed(oldPlanId, entity.getPlanId())) {
                savePlanChangeHistory(entity, oldPlanId, context.getOperatorId(), updatedAt);
            }

            SubscriptionResponse response = buildResponse(entity, request, oldPlan);

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
            throw AppException.of(ErrorCodes.INTERNAL_ERROR,
                    e.getMessage() != null && !e.getMessage().isBlank() ? e.getMessage() : "Failed to update subscription");
        }
    }

    private SubscriptionResponse buildResponse(SubscriptionEntity entity, SubscriptionUpdateRequest request, PlanEntity oldPlan) {
        SubscriptionResponse response = new SubscriptionResponse();
        response.setSubscriptionId(entity.getSubscriptionId());
        response.setStatus(entity.getStatus());
        response.setPlanCode(resolvePlanCode(entity.getPlanId()));
        response.setBillingCycle(entity.getBillingCycle());
        response.setCurrentPeriodStart(entity.getCurrentPeriodStart());
        response.setCurrentPeriodEnd(entity.getCurrentPeriodEnd());
        response.setAutoRenew(entity.getAutoRenew());

        if (oldPlan != null && StringUtils.hasText(request.getPlanCode())) {
            PlanEntity newPlan = planMapper.selectByPrimaryKey(entity.getPlanId());
            if (newPlan != null && !oldPlan.getPlanId().equals(newPlan.getPlanId())) {
                ProrateService.ProrateResult prorate = prorateService.calculate(entity, oldPlan, newPlan);
                response.setProrateCreditVnd(prorate.getCreditVnd() > 0 ? prorate.getCreditVnd() : null);
                response.setProrateChargeVnd(prorate.getChargeVnd() > 0 ? prorate.getChargeVnd() : null);
            }
        }
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
}
