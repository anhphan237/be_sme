package com.sme.be_sme.modules.platform.processor.feedback;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.platform.api.request.PlatformFeedbackResolveRequest;
import com.sme.be_sme.modules.platform.api.response.PlatformFeedbackResolveResponse;
import com.sme.be_sme.modules.platform.infrastructure.mapper.FeedbackMapper;
import com.sme.be_sme.modules.platform.infrastructure.persistence.entity.FeedbackEntity;
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
public class PlatformFeedbackResolveProcessor extends BaseBizProcessor<BizContext> {

    private final ObjectMapper objectMapper;
    private final FeedbackMapper feedbackMapper;

    @Override
    protected Object doProcess(BizContext context, JsonNode payload) {
        PlatformFeedbackResolveRequest request = objectMapper.convertValue(payload, PlatformFeedbackResolveRequest.class);

        if (!StringUtils.hasText(request.getFeedbackId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "feedbackId is required");
        }

        FeedbackEntity feedback = feedbackMapper.selectByPrimaryKey(request.getFeedbackId());
        if (feedback == null) {
            throw AppException.of(ErrorCodes.NOT_FOUND, "Feedback not found");
        }

        Date now = new Date();
        feedback.setStatus("RESOLVED");
        feedback.setResolvedAt(now);
        feedback.setResolvedBy(context.getOperatorId());
        feedback.setUpdatedAt(now);
        feedbackMapper.updateByPrimaryKey(feedback);

        PlatformFeedbackResolveResponse response = new PlatformFeedbackResolveResponse();
        response.setFeedbackId(feedback.getFeedbackId());
        response.setStatus(feedback.getStatus());
        return response;
    }
}
