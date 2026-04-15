package com.sme.be_sme.modules.platform.processor.feedback;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.identity.infrastructure.mapper.UserMapper;
import com.sme.be_sme.modules.identity.infrastructure.persistence.entity.UserEntity;
import com.sme.be_sme.modules.notification.service.NotificationCreateParams;
import com.sme.be_sme.modules.notification.service.NotificationService;
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
    private final NotificationService notificationService;
    private final UserMapper userMapper;

    @Override
    protected Object doProcess(BizContext context, JsonNode payload) {
        PlatformFeedbackResolveRequest request = objectMapper.convertValue(payload, PlatformFeedbackResolveRequest.class);

        if (!StringUtils.hasText(request.getFeedbackId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "feedbackId is required");
        }

        FeedbackEntity feedback = feedbackMapper.selectByPrimaryKey(request.getFeedbackId().trim());
        if (feedback == null) {
            throw AppException.of(ErrorCodes.NOT_FOUND, "Feedback not found");
        }

        Date now = new Date();
        feedback.setStatus("RESOLVED");
        feedback.setResolvedAt(now);
        feedback.setResolvedBy(context.getOperatorId());
        feedback.setUpdatedAt(now);

        int updated = feedbackMapper.updateByPrimaryKey(feedback);
        if (updated != 1) {
            throw AppException.of(ErrorCodes.INTERNAL_ERROR, "resolve feedback failed");
        }

        notifyFeedbackSubmitter(feedback);

        PlatformFeedbackResolveResponse response = new PlatformFeedbackResolveResponse();
        response.setFeedbackId(feedback.getFeedbackId());
        response.setStatus(feedback.getStatus());
        return response;
    }

    private void notifyFeedbackSubmitter(FeedbackEntity feedback) {
        if (!StringUtils.hasText(feedback.getUserId())) {
            return;
        }

        String resolverName = "Platform admin";
        if (StringUtils.hasText(feedback.getResolvedBy())) {
            UserEntity resolver = userMapper.selectByPrimaryKey(feedback.getResolvedBy());
            if (resolver != null && StringUtils.hasText(resolver.getFullName())) {
                resolverName = resolver.getFullName();
            }
        }

        String subject = StringUtils.hasText(feedback.getSubject()) ? feedback.getSubject().trim() : "Feedback";

        NotificationCreateParams params = NotificationCreateParams.builder()
                .companyId(feedback.getCompanyId())
                .userId(feedback.getUserId())
                .type("PLATFORM_FEEDBACK_RESOLVED")
                .title("Feedback resolved")
                .content(resolverName + " resolved your feedback: " + subject)
                .refType("FEEDBACK")
                .refId(feedback.getFeedbackId())
                .sendEmail(false)
                .build();

        notificationService.create(params);
    }
}