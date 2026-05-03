package com.sme.be_sme.modules.billing.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.billing.api.request.SubscriptionHistoryRequest;
import com.sme.be_sme.modules.billing.api.response.SubscriptionHistoryResponse;
import com.sme.be_sme.modules.billing.infrastructure.mapper.PlanMapper;
import com.sme.be_sme.modules.billing.support.SubscriptionHistoryQuerySupport;
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
public class SubscriptionHistoryProcessor extends BaseBizProcessor<BizContext> {
    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    private final ObjectMapper objectMapper;
    private final SubscriptionPlanHistoryMapper subscriptionPlanHistoryMapper;
    private final PlanMapper planMapper;

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
                .selectByCompanyAndPeriod(companyId, subscriptionId, fromTs, toTs, size, offset);

        Map<String, String> planCodeById = loadPlanCodeMap(companyId);

        List<SubscriptionHistoryResponse.Item> items = rows.stream()
                .filter(Objects::nonNull)
                .map(row -> toItem(row, planCodeById))
                .toList();

        SubscriptionHistoryResponse response = new SubscriptionHistoryResponse();
        response.setItems(items);
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

    private Map<String, String> loadPlanCodeMap(String companyId) {
        Map<String, String> map = new HashMap<>();
        for (PlanEntity plan : planMapper.selectAll()) {
            if (plan == null || !StringUtils.hasText(plan.getPlanId())) {
                continue;
            }
            if (companyId.equals(plan.getCompanyId()) || plan.getCompanyId() == null) {
                map.put(plan.getPlanId(), plan.getCode());
            }
        }
        return map;
    }

    private static SubscriptionHistoryResponse.Item toItem(SubscriptionPlanHistoryEntity row, Map<String, String> planCodeById) {
        SubscriptionHistoryResponse.Item item = new SubscriptionHistoryResponse.Item();
        item.setHistoryId(row.getSubscriptionPlanHistoryId());
        item.setSubscriptionId(row.getSubscriptionId());
        item.setOldPlanCode(resolvePlanCode(planCodeById, row.getOldPlanId()));
        item.setNewPlanCode(resolvePlanCode(planCodeById, row.getNewPlanId()));
        item.setBillingCycle(row.getBillingCycle());
        item.setChangedBy(row.getChangedBy());
        item.setChangedAt(row.getChangedAt());
        item.setEffectiveFrom(row.getEffectiveFrom());
        item.setEffectiveTo(row.getEffectiveTo());
        return item;
    }

    private static String resolvePlanCode(Map<String, String> planCodeById, String planId) {
        if (!StringUtils.hasText(planId)) {
            return null;
        }
        return planCodeById.get(planId);
    }
}
