package com.sme.be_sme.modules.billing.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.billing.api.request.UsageSummaryRequest;
import com.sme.be_sme.modules.billing.api.response.UsageSummaryItemResponse;
import com.sme.be_sme.modules.billing.api.response.UsageSummaryResponse;
import com.sme.be_sme.modules.billing.infrastructure.mapper.UsageMonthlyMapper;
import com.sme.be_sme.modules.billing.infrastructure.persistence.entity.UsageMonthlyEntity;
import com.sme.be_sme.shared.constant.ErrorCodes;
import com.sme.be_sme.shared.exception.AppException;
import com.sme.be_sme.shared.gateway.core.BaseBizProcessor;
import com.sme.be_sme.shared.gateway.core.BizContext;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class UsageSummaryProcessor extends BaseBizProcessor<BizContext> {

    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

    private final ObjectMapper objectMapper;
    private final UsageMonthlyMapper usageMonthlyMapper;

    @Override
    protected Object doProcess(BizContext context, JsonNode payload) {
        UsageSummaryRequest request = objectMapper.convertValue(payload, UsageSummaryRequest.class);
        validate(context, request);

        String companyId = context.getTenantId().trim();
        String month = resolveMonth(request == null ? null : request.getMonth());
        String subscriptionId = request == null ? null : request.getSubscriptionId();

        List<UsageSummaryItemResponse> items = usageMonthlyMapper.selectAll().stream()
                .filter(Objects::nonNull)
                .filter(row -> companyId.equals(row.getCompanyId()))
                .filter(row -> month.equals(row.getMonth()))
                .filter(row -> !StringUtils.hasText(subscriptionId)
                        || subscriptionId.trim().equals(row.getSubscriptionId()))
                .map(this::toItem)
                .collect(Collectors.toList());

        UsageSummaryResponse response = new UsageSummaryResponse();
        response.setMonth(month);
        response.setItems(items);
        return response;
    }

    private static void validate(BizContext context, UsageSummaryRequest request) {
        if (context == null || !StringUtils.hasText(context.getTenantId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "tenantId is required");
        }
        if (request == null || !StringUtils.hasText(request.getMonth())) {
            return;
        }
        parseMonth(request.getMonth());
    }

    private static String resolveMonth(String month) {
        if (!StringUtils.hasText(month)) {
            return YearMonth.now().format(MONTH_FORMATTER);
        }
        parseMonth(month);
        return month.trim();
    }

    private static void parseMonth(String month) {
        try {
            YearMonth.parse(month.trim(), MONTH_FORMATTER);
        } catch (DateTimeParseException ex) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "month must be yyyy-MM");
        }
    }

    private UsageSummaryItemResponse toItem(UsageMonthlyEntity entity) {
        UsageSummaryItemResponse response = new UsageSummaryItemResponse();
        response.setSubscriptionId(entity.getSubscriptionId());
        response.setOnboardedEmployeeCount(entity.getOnboardedEmployeeCount());
        return response;
    }
}
