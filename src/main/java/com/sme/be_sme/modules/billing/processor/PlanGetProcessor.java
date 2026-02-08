package com.sme.be_sme.modules.billing.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.billing.api.request.PlanGetRequest;
import com.sme.be_sme.modules.billing.api.response.PlanGetResponse;
import com.sme.be_sme.modules.billing.infrastructure.mapper.PlanMapper;
import com.sme.be_sme.modules.billing.infrastructure.mapper.SubscriptionMapper;
import com.sme.be_sme.modules.billing.infrastructure.persistence.entity.PlanEntity;
import com.sme.be_sme.modules.billing.infrastructure.persistence.entity.SubscriptionEntity;
import com.sme.be_sme.shared.constant.ErrorCodes;
import com.sme.be_sme.shared.exception.AppException;
import com.sme.be_sme.shared.gateway.core.BaseBizProcessor;
import com.sme.be_sme.shared.gateway.core.BizContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class PlanGetProcessor extends BaseBizProcessor<BizContext> {

    private final ObjectMapper objectMapper;
    private final PlanMapper planMapper;
    private final SubscriptionMapper subscriptionMapper;

    @Override
    protected Object doProcess(BizContext context, JsonNode payload) {
        PlanGetRequest request = objectMapper.convertValue(payload, PlanGetRequest.class);
        validate(context, request);

        String companyId = context.getTenantId();
        String planId = request != null && StringUtils.hasText(request.getPlanId())
                ? request.getPlanId().trim()
                : resolvePlanIdFromSubscription(companyId);

        if (!StringUtils.hasText(planId)) {
            throw AppException.of(ErrorCodes.NOT_FOUND, "plan not found for tenant");
        }

        PlanEntity plan = planMapper.selectByPrimaryKey(planId);
        if (plan == null) {
            throw AppException.of(ErrorCodes.NOT_FOUND, "plan not found");
        }
        if (StringUtils.hasText(plan.getCompanyId()) && !plan.getCompanyId().equals(companyId)) {
            throw AppException.of(ErrorCodes.FORBIDDEN, "plan does not belong to tenant");
        }

        PlanGetResponse response = new PlanGetResponse();
        response.setPlanId(plan.getPlanId());
        response.setCode(plan.getCode());
        response.setName(plan.getName());
        response.setEmployeeLimitPerMonth(plan.getEmployeeLimitPerMonth());
        response.setPriceVndMonthly(plan.getPriceVndMonthly());
        response.setPriceVndYearly(plan.getPriceVndYearly());
        response.setStatus(plan.getStatus());
        return response;
    }

    private String resolvePlanIdFromSubscription(String companyId) {
        List<SubscriptionEntity> list = subscriptionMapper.selectAll();
        if (list == null) return null;
        SubscriptionEntity current = list.stream()
                .filter(Objects::nonNull)
                .filter(s -> companyId.equals(s.getCompanyId()))
                .filter(s -> "ACTIVE".equalsIgnoreCase(trimLower(s.getStatus())))
                .max(Comparator.comparing(SubscriptionEntity::getUpdatedAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .orElse(null);
        return current == null ? null : current.getPlanId();
    }

    private static void validate(BizContext context, PlanGetRequest request) {
        if (context == null || !StringUtils.hasText(context.getTenantId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "tenantId is required");
        }
    }

    private static String trimLower(String value) {
        return value == null ? null : value.trim().toLowerCase(Locale.ROOT);
    }
}
