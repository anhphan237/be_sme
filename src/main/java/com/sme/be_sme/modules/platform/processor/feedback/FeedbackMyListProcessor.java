package com.sme.be_sme.modules.platform.processor.feedback;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.platform.api.request.FeedbackMyListRequest;
import com.sme.be_sme.modules.platform.api.response.FeedbackMyListResponse;
import com.sme.be_sme.modules.platform.api.response.FeedbackMyListResponse.FeedbackItem;
import com.sme.be_sme.modules.platform.infrastructure.dto.FeedbackViewRow;
import com.sme.be_sme.modules.platform.infrastructure.mapper.FeedbackMapper;
import com.sme.be_sme.shared.gateway.core.BaseBizProcessor;
import com.sme.be_sme.shared.gateway.core.BizContext;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class FeedbackMyListProcessor extends BaseBizProcessor<BizContext> {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_SIZE = 10;
    private static final int MAX_SIZE = 50;

    private final ObjectMapper objectMapper;
    private final FeedbackMapper feedbackMapper;

    @Override
    protected Object doProcess(BizContext context, JsonNode payload) {
        FeedbackMyListRequest request =
                payload == null || payload.isNull()
                        ? new FeedbackMyListRequest()
                        : objectMapper.convertValue(payload, FeedbackMyListRequest.class);

        String companyId = context.getTenantId();
        String userId = context.getOperatorId();

        if (!StringUtils.hasText(companyId)) {
            throw new IllegalArgumentException("companyId is required");
        }

        if (!StringUtils.hasText(userId)) {
            throw new IllegalArgumentException("userId is required");
        }

        int page = request.getPage() != null && request.getPage() > 0
                ? request.getPage()
                : DEFAULT_PAGE;

        int size = request.getSize() != null && request.getSize() > 0
                ? Math.min(request.getSize(), MAX_SIZE)
                : DEFAULT_SIZE;

        int offset = (page - 1) * size;

        String status = normalizeStatus(request.getStatus());
        String keyword = normalize(request.getKeyword());

        List<FeedbackViewRow> rows = feedbackMapper.selectMyFeedbackPage(
                companyId,
                userId,
                status,
                keyword,
                size,
                offset
        );

        long total = feedbackMapper.countMyFeedback(
                companyId,
                userId,
                status,
                keyword
        );

        FeedbackMyListResponse response = new FeedbackMyListResponse();
        response.setItems(mapItems(rows));
        response.setTotal(total);
        response.setPage(page);
        response.setSize(size);

        return response;
    }

    private List<FeedbackItem> mapItems(List<FeedbackViewRow> rows) {
        List<FeedbackItem> items = new ArrayList<>();

        if (rows == null || rows.isEmpty()) {
            return items;
        }

        for (FeedbackViewRow row : rows) {
            FeedbackItem item = new FeedbackItem();

            item.setFeedbackId(row.getFeedbackId());

            item.setSubject(row.getSubject());
            item.setContent(row.getContent());
            item.setStatus(row.getStatus());

            item.setResolvedAt(row.getResolvedAt());
            item.setResolvedBy(row.getResolvedBy());
            item.setResolvedByName(row.getResolvedByName());
            item.setResolutionNote(row.getResolutionNote());

            item.setCreatedAt(row.getCreatedAt());
            item.setUpdatedAt(row.getUpdatedAt());

            items.add(item);
        }

        return items;
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String normalizeStatus(String value) {
        return StringUtils.hasText(value) ? value.trim().toUpperCase() : null;
    }
}