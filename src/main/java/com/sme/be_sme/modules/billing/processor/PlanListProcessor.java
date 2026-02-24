package com.sme.be_sme.modules.billing.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.billing.api.request.PlanListRequest;
import com.sme.be_sme.modules.billing.api.response.PlanListResponse;
import com.sme.be_sme.modules.billing.api.response.PlanSummaryResponse;
import com.sme.be_sme.modules.billing.infrastructure.mapper.PlanMapper;
import com.sme.be_sme.modules.billing.infrastructure.persistence.entity.PlanEntity;
import com.sme.be_sme.shared.constant.ErrorCodes;
import com.sme.be_sme.shared.exception.AppException;
import com.sme.be_sme.shared.gateway.core.BaseBizProcessor;
import com.sme.be_sme.shared.gateway.core.BizContext;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class PlanListProcessor extends BaseBizProcessor<BizContext> {

    private final ObjectMapper objectMapper;
    private final PlanMapper planMapper;

    @Override
    protected Object doProcess(BizContext context, JsonNode payload) {
        PlanListRequest request = objectMapper.convertValue(payload, PlanListRequest.class);
        validate(context);

        String companyId = context.getTenantId().trim();
        String status = request == null ? null : request.getStatus();
        String statusNormalized = status == null ? null : status.trim().toLowerCase(Locale.ROOT);

        List<PlanSummaryResponse> plans = planMapper.selectAll().stream()
                .filter(Objects::nonNull)
                .filter(plan -> companyId.equals(plan.getCompanyId()) || plan.getCompanyId() == null)
                .filter(plan -> !StringUtils.hasText(statusNormalized)
                        || (plan.getStatus() != null && plan.getStatus().trim().toLowerCase(Locale.ROOT).equals(statusNormalized)))
                .sorted(Comparator.comparing(p -> p.getPriceVndMonthly() == null ? Integer.MAX_VALUE : p.getPriceVndMonthly()))
                .map(this::toSummary)
                .collect(Collectors.toList());

        PlanListResponse response = new PlanListResponse();
        response.setPlans(plans);
        return response;
    }

    private static void validate(BizContext context) {
        if (context == null || !StringUtils.hasText(context.getTenantId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "tenantId is required");
        }
    }

    private PlanSummaryResponse toSummary(PlanEntity entity) {
        PlanSummaryResponse response = new PlanSummaryResponse();
        response.setPlanId(entity.getPlanId());
        response.setCode(entity.getCode());
        response.setName(entity.getName());
        response.setEmployeeLimitPerMonth(entity.getEmployeeLimitPerMonth());
        response.setPriceVndMonthly(entity.getPriceVndMonthly());
        response.setPriceVndYearly(entity.getPriceVndYearly());
        response.setStatus(entity.getStatus());
        return response;
    }
}
