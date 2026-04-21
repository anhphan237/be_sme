package com.sme.be_sme.modules.platform.processor.monitoring;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.platform.api.request.PlatformErrorLogListRequest;
import com.sme.be_sme.modules.platform.api.response.PlatformErrorLogListResponse;
import com.sme.be_sme.modules.platform.api.response.PlatformErrorLogListResponse.ErrorLogItem;
import com.sme.be_sme.modules.platform.infrastructure.mapper.ErrorLogMapper;
import com.sme.be_sme.modules.platform.infrastructure.persistence.entity.ErrorLogEntity;
import com.sme.be_sme.shared.gateway.core.BaseBizProcessor;
import com.sme.be_sme.shared.gateway.core.BizContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class PlatformErrorLogListProcessor extends BaseBizProcessor<BizContext> {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    private final ObjectMapper objectMapper;
    private final ErrorLogMapper errorLogMapper;

    @Override
    protected Object doProcess(BizContext context, JsonNode payload) {
        PlatformErrorLogListRequest request = payload == null || payload.isNull()
                ? new PlatformErrorLogListRequest()
                : objectMapper.convertValue(payload, PlatformErrorLogListRequest.class);

        int page = normalizePage(request.getPage());
        int size = normalizeSize(request.getSize());
        int offset = page * size;

        String keyword = trimToNull(request.getKeyword());
        String errorCode = normalizeUpper(request.getErrorCode());
        String severity = normalizeUpper(request.getSeverity());
        String status = normalizeUpper(request.getStatus());
        String operationType = trimToNull(request.getOperationType());
        String companyId = trimToNull(request.getCompanyId());
        String actorRole = normalizeUpper(request.getActorRole());
        String startDate = trimToNull(request.getStartDate());
        String endDate = trimToNull(request.getEndDate());

        int total = errorLogMapper.countPage(
                keyword,
                errorCode,
                severity,
                status,
                operationType,
                companyId,
                actorRole,
                startDate,
                endDate
        );

        List<ErrorLogEntity> logs = errorLogMapper.selectPage(
                keyword,
                errorCode,
                severity,
                status,
                operationType,
                companyId,
                actorRole,
                startDate,
                endDate,
                size,
                offset
        );

        List<ErrorLogItem> items = new ArrayList<>();

        for (ErrorLogEntity entity : logs) {
            ErrorLogItem item = new ErrorLogItem();

            item.setErrorId(entity.getErrorId());
            item.setErrorCode(entity.getErrorCode());
            item.setMessage(entity.getMessage());
            item.setStackTrace(entity.getStackTrace());
            item.setRequestId(entity.getRequestId());
            item.setCreatedAt(entity.getCreatedAt());

            item.setOperationType(entity.getOperationType());
            item.setTenantId(entity.getTenantId());
            item.setCompanyId(entity.getCompanyId());
            item.setActorUserId(entity.getActorUserId());
            item.setActorRole(entity.getActorRole());
            item.setSeverity(entity.getSeverity());
            item.setStatus(entity.getStatus());
            item.setPayloadSnapshot(entity.getPayloadSnapshot());

            item.setResolvedAt(entity.getResolvedAt());
            item.setResolvedBy(entity.getResolvedBy());
            item.setResolutionNote(entity.getResolutionNote());

            items.add(item);
        }

        PlatformErrorLogListResponse response = new PlatformErrorLogListResponse();
        response.setItems(items);
        response.setTotal(total);
        response.setPage(page);
        response.setSize(size);

        return response;
    }

    private static int normalizePage(Integer page) {
        if (page == null || page < 0) {
            return DEFAULT_PAGE;
        }
        return page;
    }

    private static int normalizeSize(Integer size) {
        if (size == null || size <= 0) {
            return DEFAULT_SIZE;
        }
        return Math.min(size, MAX_SIZE);
    }

    private static String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private static String normalizeUpper(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim().toUpperCase();
    }
}