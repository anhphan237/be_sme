package com.sme.be_sme.modules.billing.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.billing.api.request.SubscriptionGetCurrentRequest;
import com.sme.be_sme.modules.billing.api.response.SubscriptionCurrentResponse;
import com.sme.be_sme.modules.billing.infrastructure.mapper.PlanMapper;
import com.sme.be_sme.modules.billing.infrastructure.mapper.SubscriptionMapper;
import com.sme.be_sme.modules.billing.infrastructure.persistence.entity.PlanEntity;
import com.sme.be_sme.modules.billing.infrastructure.persistence.entity.SubscriptionEntity;
import com.sme.be_sme.shared.constant.ErrorCodes;
import com.sme.be_sme.shared.exception.AppException;
import com.sme.be_sme.shared.gateway.core.BaseBizProcessor;
import com.sme.be_sme.shared.gateway.core.BizContext;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class SubscriptionGetCurrentProcessor extends BaseBizProcessor<BizContext> {

    private final ObjectMapper objectMapper;
    private final SubscriptionMapper subscriptionMapper;
    private final PlanMapper planMapper;

    @Override
    protected Object doProcess(BizContext context, JsonNode payload) {
        SubscriptionGetCurrentRequest request = objectMapper.convertValue(payload, SubscriptionGetCurrentRequest.class);
        validate(context, request);

        String companyId = context.getTenantId().trim();
        List<SubscriptionEntity> subscriptions = subscriptionMapper.selectAll().stream()
                .filter(Objects::nonNull)
                .filter(row -> companyId.equals(row.getCompanyId()))
                .toList();

        SubscriptionEntity current = pickCurrent(subscriptions);
        if (current == null) {
            throw AppException.of(ErrorCodes.NOT_FOUND, "subscription not found");
        }

        SubscriptionCurrentResponse response = new SubscriptionCurrentResponse();
        response.setSubscriptionId(current.getSubscriptionId());
        response.setStatus(current.getStatus());
        response.setBillingCycle(current.getBillingCycle());
        response.setCurrentPeriodStart(current.getCurrentPeriodStart());
        response.setCurrentPeriodEnd(current.getCurrentPeriodEnd());
        response.setAutoRenew(current.getAutoRenew());
        response.setPlanCode(resolvePlanCode(current.getPlanId()));
        return response;
    }

    private static void validate(BizContext context, SubscriptionGetCurrentRequest request) {
        if (context == null || !StringUtils.hasText(context.getTenantId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "tenantId is required");
        }
        if (request == null) {
            return;
        }
        if (StringUtils.hasText(request.getCompanyId())
                && !context.getTenantId().trim().equals(request.getCompanyId().trim())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "companyId does not match tenant");
        }
    }

    private SubscriptionEntity pickCurrent(List<SubscriptionEntity> subscriptions) {
        if (subscriptions == null || subscriptions.isEmpty()) {
            return null;
        }
        Comparator<Date> dateComparator = Comparator.nullsLast(Comparator.naturalOrder());
        Comparator<SubscriptionEntity> byUpdated = Comparator.comparing(SubscriptionEntity::getUpdatedAt, dateComparator);
        Comparator<SubscriptionEntity> byCreated = Comparator.comparing(SubscriptionEntity::getCreatedAt, dateComparator);
        Comparator<SubscriptionEntity> byRecency = byUpdated.thenComparing(byCreated);

        return subscriptions.stream()
                .filter(row -> "ACTIVE".equalsIgnoreCase(trimLower(row.getStatus())))
                .max(byRecency)
                .orElseGet(() -> subscriptions.stream().max(byRecency).orElse(null));
    }

    private String resolvePlanCode(String planId) {
        if (!StringUtils.hasText(planId)) {
            return null;
        }
        PlanEntity plan = planMapper.selectByPrimaryKey(planId);
        return plan == null ? null : plan.getCode();
    }

    private static String trimLower(String value) {
        return value == null ? null : value.trim().toLowerCase(Locale.ROOT);
    }
}
