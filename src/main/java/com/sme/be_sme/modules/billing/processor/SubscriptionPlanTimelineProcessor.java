package com.sme.be_sme.modules.billing.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.billing.api.request.SubscriptionHistoryRequest;
import com.sme.be_sme.modules.billing.api.response.SubscriptionPlanTimelineResponse;
import com.sme.be_sme.modules.billing.infrastructure.mapper.PlanMapper;
import com.sme.be_sme.modules.billing.support.SubscriptionHistoryQuerySupport;
import com.sme.be_sme.modules.billing.support.SubscriptionPlanHistoryActorNames;
import com.sme.be_sme.modules.identity.infrastructure.mapper.UserMapperExt;
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
    private final UserMapperExt userMapperExt;

    @Override
    protected Object doProcess(BizContext context, JsonNode payload) {
        SubscriptionHistoryRequest request = SubscriptionHistoryQuerySupport.parseRequest(payload, objectMapper);
        validate(context, request);

        String companyId = context.getTenantId().trim();
        int page = request == null || request.getPage() == null ? DEFAULT_PAGE : Math.max(request.getPage(), 0);
        int size = request == null || request.getSize() == null ? DEFAULT_SIZE : Math.min(Math.max(request.getSize(), 1), MAX_SIZE);
        int offset = page * size;

        SubscriptionHistoryQuerySupport.DateRange range = SubscriptionHistoryQuerySupport.resolve(request);
        Date fromTs = range.fromTs();
        Date toTs = range.toTs();

        String subscriptionId = request != null && StringUtils.hasText(request.getSubscriptionId())
                ? request.getSubscriptionId().trim() : null;

        int total = subscriptionPlanHistoryMapper.countByCompanyAndPeriod(companyId, subscriptionId, fromTs, toTs);
        List<SubscriptionPlanHistoryEntity> rows = subscriptionPlanHistoryMapper
                .selectPlanTimelineByCompany(companyId, subscriptionId, fromTs, toTs, size, offset);

        Map<String, PlanEntity> planById = loadPlanMap(companyId);
        Map<String, String> changedByDisplayNames =
                SubscriptionPlanHistoryActorNames.loadDisplayNamesByChangedBy(companyId, rows, userMapperExt);

        List<SubscriptionPlanTimelineResponse.Segment> segments = rows.stream()
                .filter(Objects::nonNull)
                .map(row -> toSegment(row, planById, changedByDisplayNames))
                .toList();

        SubscriptionPlanTimelineResponse response = new SubscriptionPlanTimelineResponse();
        response.setSegments(segments);
        response.setTotal(total);
        response.setPage(page);
        response.setSize(size);
        response.setTotalPages(totalPages(total, size));
        return response;
    }

    private static int totalPages(int total, int size) {
        if (total <= 0 || size <= 0) {
            return 0;
        }
        return (total + size - 1) / size;
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
                                                                        Map<String, PlanEntity> planById,
                                                                        Map<String, String> changedByDisplayNames) {
        SubscriptionPlanTimelineResponse.Segment seg = new SubscriptionPlanTimelineResponse.Segment();
        seg.setHistoryId(row.getSubscriptionPlanHistoryId());
        seg.setSubscriptionId(row.getSubscriptionId());
        seg.setPlanId(row.getNewPlanId());
        seg.setBillingCycle(row.getBillingCycle());

        Date start = row.getEffectiveFrom() != null ? row.getEffectiveFrom() : row.getChangedAt();
        seg.setEffectiveFrom(start);
        seg.setEffectiveTo(row.getEffectiveTo());
        seg.setChangedAt(row.getChangedAt());
        String changedBy = row.getChangedBy();
        seg.setChangedBy(changedBy);
        if (StringUtils.hasText(changedBy)) {
            seg.setChangedByName(changedByDisplayNames.get(changedBy.trim()));
        }

        PlanEntity plan = StringUtils.hasText(row.getNewPlanId()) ? planById.get(row.getNewPlanId()) : null;
        if (plan != null) {
            seg.setPlanCode(plan.getCode());
            seg.setPlanName(plan.getName());
        }
        return seg;
    }
}
