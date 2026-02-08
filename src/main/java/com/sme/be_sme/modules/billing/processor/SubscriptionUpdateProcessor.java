package com.sme.be_sme.modules.billing.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.billing.api.request.SubscriptionUpdateRequest;
import com.sme.be_sme.modules.billing.api.response.SubscriptionResponse;
import com.sme.be_sme.modules.billing.infrastructure.mapper.PlanMapper;
import com.sme.be_sme.modules.billing.infrastructure.mapper.SubscriptionMapper;
import com.sme.be_sme.modules.billing.infrastructure.persistence.entity.PlanEntity;
import com.sme.be_sme.modules.billing.infrastructure.persistence.entity.SubscriptionEntity;
import com.sme.be_sme.modules.billing.service.ProrateService;
import com.sme.be_sme.shared.constant.ErrorCodes;
import com.sme.be_sme.shared.exception.AppException;
import com.sme.be_sme.shared.gateway.core.BaseBizProcessor;
import com.sme.be_sme.shared.gateway.core.BizContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Date;

@Component
@RequiredArgsConstructor
public class SubscriptionUpdateProcessor extends BaseBizProcessor<BizContext> {

    private final ObjectMapper objectMapper;
    private final SubscriptionMapper subscriptionMapper;
    private final PlanMapper planMapper;
    private final ProrateService prorateService;

    @Override
    protected Object doProcess(BizContext context, JsonNode payload) {
        SubscriptionUpdateRequest request = objectMapper.convertValue(payload, SubscriptionUpdateRequest.class);
        validate(context, request);

        SubscriptionEntity entity = subscriptionMapper.selectByPrimaryKey(request.getSubscriptionId().trim());
        if (entity == null || !context.getTenantId().trim().equals(entity.getCompanyId())) {
            throw AppException.of(ErrorCodes.NOT_FOUND, "subscription not found");
        }

        PlanEntity oldPlan = null;
        if (StringUtils.hasText(entity.getPlanId())) {
            oldPlan = planMapper.selectByPrimaryKey(entity.getPlanId());
        }

        if (StringUtils.hasText(request.getPlanCode())) {
            PlanEntity newPlan = findPlanByCode(context.getTenantId().trim(), request.getPlanCode().trim());
            if (newPlan == null) {
                throw AppException.of(ErrorCodes.NOT_FOUND, "plan not found");
            }
            entity.setPlanId(newPlan.getPlanId());
        }

        if (StringUtils.hasText(request.getStatus())) {
            entity.setStatus(request.getStatus().trim());
        }

        entity.setUpdatedAt(new Date());
        int updated = subscriptionMapper.updateByPrimaryKey(entity);
        if (updated != 1) {
            throw AppException.of(ErrorCodes.INTERNAL_ERROR, "update subscription failed");
        }

        SubscriptionResponse response = new SubscriptionResponse();
        response.setSubscriptionId(entity.getSubscriptionId());
        response.setStatus(entity.getStatus());
        response.setPlanCode(resolvePlanCode(entity.getPlanId()));

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
}
