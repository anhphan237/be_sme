package com.sme.be_sme.modules.platform.processor.feedback;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.identity.infrastructure.mapper.UserMapper;
import com.sme.be_sme.modules.identity.infrastructure.mapper.UserMapperExt;
import com.sme.be_sme.modules.identity.infrastructure.persistence.entity.UserEntity;
import com.sme.be_sme.modules.notification.service.NotificationCreateParams;
import com.sme.be_sme.modules.notification.service.NotificationService;
import com.sme.be_sme.modules.platform.api.request.FeedbackSubmitRequest;
import com.sme.be_sme.modules.platform.api.response.FeedbackSubmitResponse;
import com.sme.be_sme.modules.platform.infrastructure.mapper.FeedbackMapper;
import com.sme.be_sme.modules.platform.infrastructure.persistence.entity.FeedbackEntity;
import com.sme.be_sme.shared.constant.ErrorCodes;
import com.sme.be_sme.shared.exception.AppException;
import com.sme.be_sme.shared.gateway.core.BaseBizProcessor;
import com.sme.be_sme.shared.gateway.core.BizContext;
import com.sme.be_sme.shared.util.UuidGenerator;
import java.util.Date;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class FeedbackSubmitProcessor extends BaseBizProcessor<BizContext> {

    private final ObjectMapper objectMapper;
    private final FeedbackMapper feedbackMapper;
    private final NotificationService notificationService;
    private final UserMapperExt userMapperExt;
    private final UserMapper userMapper;

    @Override
    protected Object doProcess(BizContext context, JsonNode payload) {
        FeedbackSubmitRequest request = objectMapper.convertValue(payload, FeedbackSubmitRequest.class);

        if (!StringUtils.hasText(request.getSubject())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "subject is required");
        }
        if (!StringUtils.hasText(request.getContent())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "content is required");
        }

        Date now = new Date();
        FeedbackEntity entity = new FeedbackEntity();
        entity.setFeedbackId(UuidGenerator.generate());
        entity.setCompanyId(context.getTenantId());
        entity.setUserId(context.getOperatorId());
        entity.setSubject(request.getSubject().trim());
        entity.setContent(request.getContent().trim());
        entity.setStatus("OPEN");
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);

        int inserted = feedbackMapper.insert(entity);
        if (inserted != 1) {
            throw AppException.of(ErrorCodes.INTERNAL_ERROR, "submit feedback failed");
        }

        notifyPlatformAdmins(entity);

        FeedbackSubmitResponse response = new FeedbackSubmitResponse();
        response.setFeedbackId(entity.getFeedbackId());
        return response;
    }

    private void notifyPlatformAdmins(FeedbackEntity feedback) {
        List<String> adminUserIds = userMapperExt.selectPlatformAdminUserIds();
        if (adminUserIds == null || adminUserIds.isEmpty()) {
            return;
        }

        String senderName = "A user";
        if (StringUtils.hasText(feedback.getUserId())) {
            UserEntity sender = userMapper.selectByPrimaryKey(feedback.getUserId());
            if (sender != null && StringUtils.hasText(sender.getFullName())) {
                senderName = sender.getFullName();
            }
        }

        String subject = StringUtils.hasText(feedback.getSubject()) ? feedback.getSubject().trim() : "Feedback";

        for (String adminUserId : adminUserIds) {
            if (!StringUtils.hasText(adminUserId)) {
                continue;
            }

            NotificationCreateParams params = NotificationCreateParams.builder()
                    .companyId(feedback.getCompanyId())
                    .userId(adminUserId)
                    .type("PLATFORM_FEEDBACK_SUBMITTED")
                    .title("New feedback submitted")
                    .content(senderName + " submitted feedback: " + subject)
                    .refType("FEEDBACK")
                    .refId(feedback.getFeedbackId())
                    .sendEmail(false)
                    .build();

            notificationService.create(params);
        }
    }
}