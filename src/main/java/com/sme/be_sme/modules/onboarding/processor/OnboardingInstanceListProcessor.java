package com.sme.be_sme.modules.onboarding.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.onboarding.api.request.OnboardingInstanceListRequest;
import com.sme.be_sme.modules.onboarding.api.response.OnboardingInstanceDetailResponse;
import com.sme.be_sme.modules.onboarding.api.response.OnboardingInstanceListResponse;
import com.sme.be_sme.modules.onboarding.infrastructure.mapper.OnboardingInstanceMapper;
import com.sme.be_sme.modules.onboarding.infrastructure.persistence.entity.OnboardingInstanceEntity;
import com.sme.be_sme.shared.constant.ErrorCodes;
import com.sme.be_sme.shared.exception.AppException;
import com.sme.be_sme.shared.gateway.core.BaseBizProcessor;
import com.sme.be_sme.shared.gateway.core.BizContext;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class OnboardingInstanceListProcessor extends BaseBizProcessor<BizContext> {

    private final ObjectMapper objectMapper;
    private final OnboardingInstanceMapper onboardingInstanceMapper;

    @Override
    protected Object doProcess(BizContext context, JsonNode payload) {
        OnboardingInstanceListRequest request = objectMapper.convertValue(payload, OnboardingInstanceListRequest.class);
        validate(context);

        String companyId = context.getTenantId();
        String employeeId = request == null ? null : request.getEmployeeId();
        String status = request == null ? null : request.getStatus();
        String statusNormalized = status == null ? null : status.trim().toLowerCase(Locale.ROOT);

        List<OnboardingInstanceDetailResponse> instances = onboardingInstanceMapper.selectAll().stream()
                .filter(row -> Objects.equals(companyId, row.getCompanyId()))
                .filter(row -> !StringUtils.hasText(employeeId) || employeeId.trim().equals(row.getEmployeeId()))
                .filter(row -> !StringUtils.hasText(statusNormalized)
                        || (row.getStatus() != null && row.getStatus().trim().toLowerCase(Locale.ROOT).equals(statusNormalized)))
                .map(this::toDetailResponse)
                .collect(Collectors.toList());

        OnboardingInstanceListResponse response = new OnboardingInstanceListResponse();
        response.setInstances(instances);
        return response;
    }

    private static void validate(BizContext context) {
        if (context == null || !StringUtils.hasText(context.getTenantId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "tenantId is required");
        }
    }

    private OnboardingInstanceDetailResponse toDetailResponse(OnboardingInstanceEntity entity) {
        OnboardingInstanceDetailResponse response = new OnboardingInstanceDetailResponse();
        response.setInstanceId(entity.getOnboardingId());
        response.setEmployeeId(entity.getEmployeeId());
        response.setTemplateId(entity.getOnboardingTemplateId());
        response.setStatus(entity.getStatus());
        response.setStartDate(entity.getStartDate());
        response.setCompletedAt(entity.getCompletedAt());
        return response;
    }
}
