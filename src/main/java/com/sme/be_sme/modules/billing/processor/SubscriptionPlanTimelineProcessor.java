package com.sme.be_sme.modules.billing.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.billing.api.request.SubscriptionHistoryRequest;
import com.sme.be_sme.modules.billing.api.response.SubscriptionPlanTimelineResponse;
import com.sme.be_sme.modules.billing.infrastructure.mapper.PlanMapper;
import com.sme.be_sme.modules.billing.infrastructure.mapper.SubscriptionPlanHistoryMapper;
import com.sme.be_sme.modules.billing.infrastructure.persistence.entity.PlanEntity;
import com.sme.be_sme.modules.billing.infrastructure.persistence.entity.SubscriptionPlanHistoryEntity;
import com.sme.be_sme.shared.constant.ErrorCodes;
import com.sme.be_sme.shared.exception.AppException;
import com.sme.be_sme.shared.gateway.core.BaseBizProcessor;
import com.sme.be_sme.shared.gateway.core.BizContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class SubscriptionPlanTimelineProcessor extends BaseBizProcessor<BizContext> {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 50;
    private static final int MAX_SIZE = 500;

    private final ObjectMapper objectMapper;
    private final SubscriptionPlanHistoryMapper subscriptionPlanHistoryMapper;
    private final PlanMapper planMapper;

    @Override
    protected Object doProcess(BizContext context, JsonNode payload) {
        SubscriptionHistoryRequest request = objectMapper.convertValue(payload, SubscriptionHistoryRequest.class);
        validate(context, request);

        String companyId = context.getTenantId().trim();
        int page = request == null || request.getPage() == null ? DEFAULT_PAGE : Math.max(request.getPage(), 0);
        int size = request == null || request.getSize() == null ? DEFAULT_SIZE : Math.min(Math.max(request.getSize(), 1), MAX_SIZE);
        int offset = page * size;

        Date fromTs = parseStartOfDay(request != null ? request.getFromDate() : null, "fromDate");
        Date toTs = parseEndOfDay(request != null ? request.getToDate() : null, "toDate");
        if (fromTs != null && toTs != null && fromTs.after(toTs)) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "fromDate must be before or equal to toDate");
        }

        String subscriptionId = request != null && StringUtils.hasText(request.getSubscriptionId())
                ? request.getSubscriptionId().trim() : null;

        int total = subscriptionPlanHistoryMapper.countByCompanyAndPeriod(companyId, subscriptionId, fromTs, toTs);
        List<SubscriptionPlanHistoryEntity> rows = subscriptionPlanHistoryMapper
                .selectPlanTimelineByCompany(companyId, subscriptionId, fromTs, toTs, size, offset);

        Map<String, PlanEntity> planById = loadPlanMap(companyId);

        List<SubscriptionPlanTimelineResponse.Segment> segments = rows.stream()
                .filter(Objects::nonNull)
                .map(row -> toSegment(row, planById))
                .toList();

        SubscriptionPlanTimelineResponse response = new SubscriptionPlanTimelineResponse();
        response.setSegments(segments);
        response.setTotal(total);
        return response;
    }

    private static void validate(BizContext context, SubscriptionHistoryRequest request) {
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

    private Map<String, PlanEntity> loadPlanMap(String companyId) {
        Map<String, PlanEntity> map = new HashMap<>();
        for (PlanEntity plan : planMapper.selectAll()) {
            if (plan == null || !StringUtils.hasText(plan.getPlanId())) {
                continue;
            }
            if (companyId.equals(plan.getCompanyId()) || plan.getCompanyId() == null) {
                map.put(plan.getPlanId(), plan);
            }
        }
        return map;
    }

    private static SubscriptionPlanTimelineResponse.Segment toSegment(SubscriptionPlanHistoryEntity row,
                                                                        Map<String, PlanEntity> planById) {
        SubscriptionPlanTimelineResponse.Segment seg = new SubscriptionPlanTimelineResponse.Segment();
        seg.setHistoryId(row.getSubscriptionPlanHistoryId());
        seg.setSubscriptionId(row.getSubscriptionId());
        seg.setPlanId(row.getNewPlanId());
        seg.setBillingCycle(row.getBillingCycle());

        Date start = row.getEffectiveFrom() != null ? row.getEffectiveFrom() : row.getChangedAt();
        seg.setEffectiveFrom(start);
        seg.setEffectiveTo(row.getEffectiveTo());

        PlanEntity plan = StringUtils.hasText(row.getNewPlanId()) ? planById.get(row.getNewPlanId()) : null;
        if (plan != null) {
            seg.setPlanCode(plan.getCode());
            seg.setPlanName(plan.getName());
        }
        return seg;
    }

    private static Date parseStartOfDay(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            LocalDate date = LocalDate.parse(value.trim());
            return Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant());
        } catch (DateTimeParseException e) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, fieldName + " must be yyyy-MM-dd");
        }
    }

    private static Date parseEndOfDay(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            LocalDate date = LocalDate.parse(value.trim()).plusDays(1);
            return Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant().minusMillis(1));
        } catch (DateTimeParseException e) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, fieldName + " must be yyyy-MM-dd");
        }
    }
}
