package com.sme.be_sme.modules.platform.processor.feedback;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.platform.api.request.PlatformFeedbackResolveRequest;
import com.sme.be_sme.modules.platform.api.response.PlatformFeedbackResolveResponse;
import com.sme.be_sme.modules.platform.infrastructure.mapper.FeedbackMapper;
import com.sme.be_sme.modules.platform.infrastructure.persistence.entity.FeedbackEntity;
import com.sme.be_sme.shared.gateway.core.BaseBizProcessor;
import com.sme.be_sme.shared.gateway.core.BizContext;
import java.time.OffsetDateTime;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class PlatformFeedbackResolveProcessor extends BaseBizProcessor<BizContext> {

    private static final Set<String> ALLOWED_STATUS = Set.of(
            "OPEN",
            "IN_PROGRESS",
            "RESOLVED",
            "CLOSED"
    );

    private final ObjectMapper objectMapper;
    private final FeedbackMapper feedbackMapper;

    @Override
    protected Object doProcess(BizContext context, JsonNode payload) {
        PlatformFeedbackResolveRequest request =
                objectMapper.convertValue(payload, PlatformFeedbackResolveRequest.class);

        if (request == null) {
            throw new IllegalArgumentException("Request is required");
        }

        if (!StringUtils.hasText(request.getFeedbackId())) {
            throw new IllegalArgumentException("feedbackId is required");
        }

        String operatorId = context.getOperatorId();

        if (!StringUtils.hasText(operatorId)) {
            throw new IllegalArgumentException("operatorId is required");
        }

        FeedbackEntity feedback = feedbackMapper.selectById(request.getFeedbackId());

        if (feedback == null) {
            throw new IllegalArgumentException("Feedback not found");
        }

        String status = normalizeStatus(request.getStatus());

        if (!ALLOWED_STATUS.contains(status)) {
            throw new IllegalArgumentException("Invalid feedback status");
        }

        String resolutionNote = normalize(request.getResolutionNote());

        OffsetDateTime resolvedAt = null;
        String resolvedBy = null;

        if ("RESOLVED".equals(status) || "CLOSED".equals(status)) {
            resolvedAt = OffsetDateTime.now();
            resolvedBy = operatorId;
        }

        int updated = feedbackMapper.resolveFeedback(
                request.getFeedbackId(),
                status,
                resolvedAt,
                resolvedBy,
                resolutionNote
        );

        if (updated <= 0) {
            throw new IllegalStateException("Feedback update failed");
        }

        PlatformFeedbackResolveResponse response = new PlatformFeedbackResolveResponse();
        response.setFeedbackId(request.getFeedbackId());
        response.setStatus(status);
        response.setResolvedAt(resolvedAt);
        response.setResolvedBy(resolvedBy);
        response.setResolutionNote(resolutionNote);

        return response;
    }

    private String normalizeStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return "RESOLVED";
        }

        return status.trim().toUpperCase();
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}