package com.sme.be_sme.modules.platform.processor.feedback;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class FeedbackSubmitProcessor extends BaseBizProcessor<BizContext> {

    private final ObjectMapper objectMapper;
    private final FeedbackMapper feedbackMapper;

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
        entity.setSubject(request.getSubject());
        entity.setContent(request.getContent());
        entity.setStatus("OPEN");
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        feedbackMapper.insert(entity);

        FeedbackSubmitResponse response = new FeedbackSubmitResponse();
        response.setFeedbackId(entity.getFeedbackId());
        return response;
    }
}
