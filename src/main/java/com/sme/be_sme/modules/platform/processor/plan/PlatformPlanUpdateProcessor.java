package com.sme.be_sme.modules.platform.processor.plan;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.billing.infrastructure.mapper.PlanMapper;
import com.sme.be_sme.modules.billing.infrastructure.persistence.entity.PlanEntity;
import com.sme.be_sme.modules.platform.api.request.PlatformPlanUpdateRequest;
import com.sme.be_sme.modules.platform.api.response.PlatformPlanResponse;
import com.sme.be_sme.shared.constant.ErrorCodes;
import com.sme.be_sme.shared.exception.AppException;
import com.sme.be_sme.shared.gateway.core.BaseBizProcessor;
import com.sme.be_sme.shared.gateway.core.BizContext;
import java.util.Date;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class PlatformPlanUpdateProcessor extends BaseBizProcessor<BizContext> {

    private final ObjectMapper objectMapper;
    private final PlanMapper planMapper;

    @Override
    protected Object doProcess(BizContext context, JsonNode payload) {
        PlatformPlanUpdateRequest request =
                objectMapper.convertValue(payload, PlatformPlanUpdateRequest.class);

        if (!StringUtils.hasText(request.getPlanId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "planId is required");
        }

        PlanEntity plan = planMapper.selectByPrimaryKey(request.getPlanId());
        if (plan == null) {
            throw AppException.of(ErrorCodes.NOT_FOUND, "Plan not found");
        }

        if (request.getName() != null) {
            plan.setName(request.getName());
        }
        if (request.getEmployeeLimitPerMonth() != null) {
            plan.setEmployeeLimitPerMonth(request.getEmployeeLimitPerMonth());
        }
        if (request.getOnboardingTemplateLimit() != null) {
            plan.setOnboardingTemplateLimit(request.getOnboardingTemplateLimit());
        }
        if (request.getEventTemplateLimit() != null) {
            plan.setEventTemplateLimit(request.getEventTemplateLimit());
        }
        if (request.getDocumentLimit() != null) {
            plan.setDocumentLimit(request.getDocumentLimit());
        }
        if (request.getStorageLimitBytes() != null) {
            plan.setStorageLimitBytes(request.getStorageLimitBytes());
        }
        if (request.getPriceVndMonthly() != null) {
            plan.setPriceVndMonthly(request.getPriceVndMonthly());
        }
        if (request.getPriceVndYearly() != null) {
            plan.setPriceVndYearly(request.getPriceVndYearly());
        }
        plan.setUpdatedAt(new Date());

        planMapper.updateByPrimaryKey(plan);

        PlatformPlanResponse response = new PlatformPlanResponse();
        response.setPlanId(plan.getPlanId());
        response.setCode(plan.getCode());
        response.setName(plan.getName());
        response.setEmployeeLimitPerMonth(plan.getEmployeeLimitPerMonth());
        response.setOnboardingTemplateLimit(plan.getOnboardingTemplateLimit());
        response.setEventTemplateLimit(plan.getEventTemplateLimit());
        response.setDocumentLimit(plan.getDocumentLimit());
        response.setStorageLimitBytes(plan.getStorageLimitBytes());
        response.setStatus(plan.getStatus());
        return response;
    }
}
