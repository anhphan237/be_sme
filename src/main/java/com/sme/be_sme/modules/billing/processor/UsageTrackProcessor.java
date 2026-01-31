package com.sme.be_sme.modules.billing.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.billing.api.request.UsageTrackRequest;
import com.sme.be_sme.modules.billing.api.response.UsageTrackResponse;
import com.sme.be_sme.modules.billing.infrastructure.mapper.SubscriptionMapper;
import com.sme.be_sme.modules.billing.infrastructure.mapper.UsageMonthlyMapper;
import com.sme.be_sme.modules.billing.infrastructure.persistence.entity.SubscriptionEntity;
import com.sme.be_sme.modules.billing.infrastructure.persistence.entity.UsageMonthlyEntity;
import com.sme.be_sme.shared.constant.ErrorCodes;
import com.sme.be_sme.shared.exception.AppException;
import com.sme.be_sme.shared.gateway.core.BaseBizProcessor;
import com.sme.be_sme.shared.gateway.core.BizContext;
import com.sme.be_sme.shared.util.UuidGenerator;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class UsageTrackProcessor extends BaseBizProcessor<BizContext> {

    private final ObjectMapper objectMapper;
    private final UsageMonthlyMapper usageMonthlyMapper;
    private final SubscriptionMapper subscriptionMapper;

    @Override
    protected Object doProcess(BizContext context, JsonNode payload) {
        UsageTrackRequest request = objectMapper.convertValue(payload, UsageTrackRequest.class);
        validate(context, request);

        SubscriptionEntity subscription = subscriptionMapper.selectByPrimaryKey(request.getSubscriptionId().trim());
        if (subscription == null || !context.getTenantId().trim().equals(subscription.getCompanyId())) {
            throw AppException.of(ErrorCodes.NOT_FOUND, "subscription not found");
        }

        if (!"ONBOARDED_EMPLOYEE".equalsIgnoreCase(request.getUsageType().trim())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "usageType is not supported");
        }

        String month = YearMonth.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        UsageMonthlyEntity existing = findUsage(context.getTenantId().trim(), request.getSubscriptionId().trim(), month);
        int quantity = request.getQuantity();
        int total;

        if (existing == null) {
            UsageMonthlyEntity entity = new UsageMonthlyEntity();
            entity.setUsageMonthlyId(UuidGenerator.generate());
            entity.setCompanyId(context.getTenantId().trim());
            entity.setSubscriptionId(request.getSubscriptionId().trim());
            entity.setMonth(month);
            entity.setOnboardedEmployeeCount(quantity);
            entity.setUpdatedAt(new Date());
            int inserted = usageMonthlyMapper.insert(entity);
            if (inserted != 1) {
                throw AppException.of(ErrorCodes.INTERNAL_ERROR, "track usage failed");
            }
            total = quantity;
        } else {
            int current = existing.getOnboardedEmployeeCount() == null ? 0 : existing.getOnboardedEmployeeCount();
            total = current + quantity;
            existing.setOnboardedEmployeeCount(total);
            existing.setUpdatedAt(new Date());
            int updated = usageMonthlyMapper.updateByPrimaryKey(existing);
            if (updated != 1) {
                throw AppException.of(ErrorCodes.INTERNAL_ERROR, "track usage failed");
            }
        }

        UsageTrackResponse response = new UsageTrackResponse();
        response.setSubscriptionId(request.getSubscriptionId());
        response.setUsageType(request.getUsageType());
        response.setQuantity(total);
        return response;
    }

    private static void validate(BizContext context, UsageTrackRequest request) {
        if (context == null || !StringUtils.hasText(context.getTenantId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "tenantId is required");
        }
        if (request == null) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "payload is required");
        }
        if (!StringUtils.hasText(request.getSubscriptionId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "subscriptionId is required");
        }
        if (!StringUtils.hasText(request.getUsageType())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "usageType is required");
        }
        if (request.getQuantity() == null) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "quantity is required");
        }
        if (request.getQuantity() < 0) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "quantity must be >= 0");
        }
    }

    private UsageMonthlyEntity findUsage(String companyId, String subscriptionId, String month) {
        return usageMonthlyMapper.selectAll().stream()
                .filter(usage -> usage != null)
                .filter(usage -> companyId.equals(usage.getCompanyId()))
                .filter(usage -> subscriptionId.equals(usage.getSubscriptionId()))
                .filter(usage -> month.equals(usage.getMonth()))
                .findFirst()
                .orElse(null);
    }
}
