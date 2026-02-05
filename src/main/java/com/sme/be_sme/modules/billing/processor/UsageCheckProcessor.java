package com.sme.be_sme.modules.billing.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.billing.api.request.UsageCheckRequest;
import com.sme.be_sme.modules.billing.api.response.UsageCheckResponse;
import com.sme.be_sme.modules.billing.infrastructure.mapper.OnboardingUsageMapper;
import com.sme.be_sme.shared.constant.ErrorCodes;
import com.sme.be_sme.shared.exception.AppException;
import com.sme.be_sme.shared.gateway.core.BaseBizProcessor;
import com.sme.be_sme.shared.gateway.core.BizContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

@Component
@RequiredArgsConstructor
public class UsageCheckProcessor extends BaseBizProcessor<BizContext> {

    private final ObjectMapper objectMapper;
    private final OnboardingUsageMapper onboardingUsageMapper;

    private static final DateTimeFormatter YEAR_MONTH = DateTimeFormatter.ofPattern("yyyy-MM");

    @Override
    protected Object doProcess(BizContext context, JsonNode payload) {
        UsageCheckRequest request = payload != null ? objectMapper.convertValue(payload, UsageCheckRequest.class) : null;
        validate(context);

        String companyId = context.getTenantId();
        YearMonth ym = StringUtils.hasText(request != null ? request.getMonth() : null)
                ? YearMonth.parse(request.getMonth().trim(), YEAR_MONTH)
                : YearMonth.now();
        String month = ym.format(YEAR_MONTH);

        Date monthStart = Date.from(ym.atDay(1).atStartOfDay(ZoneId.systemDefault()).toInstant());
        Calendar cal = Calendar.getInstance(Locale.US);
        cal.setTime(monthStart);
        cal.add(Calendar.MONTH, 1);
        Date monthEnd = cal.getTime();

        int count = onboardingUsageMapper.countOnboardingInstancesByCompanyAndDateRange(companyId, monthStart, monthEnd);

        UsageCheckResponse response = new UsageCheckResponse();
        response.setCurrentUsage(count);
        response.setMonth(month);
        return response;
    }

    private static void validate(BizContext context) {
        if (context == null || !StringUtils.hasText(context.getTenantId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "tenantId is required");
        }
    }
}
