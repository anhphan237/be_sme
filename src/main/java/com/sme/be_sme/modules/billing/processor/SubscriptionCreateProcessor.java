package com.sme.be_sme.modules.billing.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.billing.api.request.SubscriptionCreateRequest;
import com.sme.be_sme.modules.billing.api.response.SubscriptionResponse;
import com.sme.be_sme.modules.billing.infrastructure.mapper.PlanMapper;
import com.sme.be_sme.modules.billing.infrastructure.mapper.SubscriptionMapper;
import com.sme.be_sme.modules.billing.infrastructure.persistence.entity.PlanEntity;
import com.sme.be_sme.modules.billing.infrastructure.persistence.entity.SubscriptionEntity;
import com.sme.be_sme.shared.constant.ErrorCodes;
import com.sme.be_sme.shared.exception.AppException;
import com.sme.be_sme.shared.gateway.core.BaseBizProcessor;
import com.sme.be_sme.shared.gateway.core.BizContext;
import com.sme.be_sme.shared.util.UuidGenerator;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class SubscriptionCreateProcessor extends BaseBizProcessor<BizContext> {

    private final ObjectMapper objectMapper;
    private final SubscriptionMapper subscriptionMapper;
    private final PlanMapper planMapper;

    @Override
    protected Object doProcess(BizContext context, JsonNode payload) {
        SubscriptionCreateRequest request = objectMapper.convertValue(payload, SubscriptionCreateRequest.class);
        validate(context, request);

        String companyId = context.getTenantId().trim();
        if (StringUtils.hasText(request.getCompanyId())
                && !companyId.equals(request.getCompanyId().trim())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "companyId does not match tenant");
        }

        PlanEntity plan = findPlanByCode(companyId, request.getPlanCode().trim());
        if (plan == null) {
            throw AppException.of(ErrorCodes.NOT_FOUND, "plan not found");
        }

        Date now = new Date();
        SubscriptionEntity entity = new SubscriptionEntity();
        entity.setSubscriptionId(UuidGenerator.generate());
        entity.setCompanyId(companyId);
        entity.setPlanId(plan.getPlanId());
        entity.setBillingCycle("MONTHLY");
        entity.setStatus("ACTIVE");
        entity.setCurrentPeriodStart(toDate(LocalDate.now()));
        entity.setCurrentPeriodEnd(toDate(LocalDate.now().plusMonths(1)));
        entity.setAutoRenew(true);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);

        int inserted = subscriptionMapper.insert(entity);
        if (inserted != 1) {
            throw AppException.of(ErrorCodes.INTERNAL_ERROR, "create subscription failed");
        }

        SubscriptionResponse response = new SubscriptionResponse();
        response.setSubscriptionId(entity.getSubscriptionId());
        response.setPlanCode(plan.getCode());
        response.setStatus(entity.getStatus());
        return response;
    }

    private static void validate(BizContext context, SubscriptionCreateRequest request) {
        if (context == null || !StringUtils.hasText(context.getTenantId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "tenantId is required");
        }
        if (request == null) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "payload is required");
        }
        if (!StringUtils.hasText(request.getPlanCode())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "planCode is required");
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

    private static Date toDate(LocalDate localDate) {
        return Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }
}
