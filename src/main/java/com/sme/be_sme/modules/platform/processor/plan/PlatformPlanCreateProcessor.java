package com.sme.be_sme.modules.platform.processor.plan;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.billing.infrastructure.mapper.PlanMapper;
import com.sme.be_sme.modules.billing.infrastructure.persistence.entity.PlanEntity;
import com.sme.be_sme.modules.platform.api.request.PlatformPlanCreateRequest;
import com.sme.be_sme.modules.platform.api.response.PlatformPlanResponse;
import com.sme.be_sme.shared.constant.ErrorCodes;
import com.sme.be_sme.shared.exception.AppException;
import com.sme.be_sme.shared.gateway.core.BaseBizProcessor;
import com.sme.be_sme.shared.gateway.core.BizContext;
import com.sme.be_sme.shared.util.UuidGenerator;
import java.util.Date;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class PlatformPlanCreateProcessor extends BaseBizProcessor<BizContext> {

    private final ObjectMapper objectMapper;
    private final PlanMapper planMapper;

    @Override
    protected Object doProcess(BizContext context, JsonNode payload) {
        PlatformPlanCreateRequest request =
                objectMapper.convertValue(payload, PlatformPlanCreateRequest.class);

        if (!StringUtils.hasText(request.getCode()) || !StringUtils.hasText(request.getName())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "code and name are required");
        }

        boolean duplicateExists = planMapper.selectAll().stream()
                .anyMatch(p -> request.getCode().equals(p.getCode()) && !"INACTIVE".equals(p.getStatus()));

        if (duplicateExists) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "A plan with this code already exists");
        }

        PlanEntity entity = new PlanEntity();
        entity.setPlanId(UuidGenerator.generate());
        entity.setCompanyId(null);
        entity.setCode(request.getCode());
        entity.setName(request.getName());
        entity.setEmployeeLimitPerMonth(request.getEmployeeLimitPerMonth());
        entity.setPriceVndMonthly(request.getPriceVndMonthly());
        entity.setPriceVndYearly(request.getPriceVndYearly());
        entity.setStatus("ACTIVE");
        entity.setCreatedAt(new Date());
        entity.setUpdatedAt(new Date());

        planMapper.insert(entity);

        PlatformPlanResponse response = new PlatformPlanResponse();
        response.setPlanId(entity.getPlanId());
        response.setCode(entity.getCode());
        response.setName(entity.getName());
        response.setStatus(entity.getStatus());
        return response;
    }
}
