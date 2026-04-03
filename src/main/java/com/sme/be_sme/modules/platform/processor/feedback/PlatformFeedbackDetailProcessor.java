package com.sme.be_sme.modules.platform.processor.feedback;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.company.infrastructure.mapper.CompanyMapper;
import com.sme.be_sme.modules.company.infrastructure.persistence.entity.CompanyEntity;
import com.sme.be_sme.modules.identity.infrastructure.mapper.UserMapper;
import com.sme.be_sme.modules.identity.infrastructure.persistence.entity.UserEntity;
import com.sme.be_sme.modules.platform.api.request.PlatformFeedbackDetailRequest;
import com.sme.be_sme.modules.platform.api.response.PlatformFeedbackDetailResponse;
import com.sme.be_sme.modules.platform.infrastructure.mapper.FeedbackMapper;
import com.sme.be_sme.modules.platform.infrastructure.persistence.entity.FeedbackEntity;
import com.sme.be_sme.shared.constant.ErrorCodes;
import com.sme.be_sme.shared.exception.AppException;
import com.sme.be_sme.shared.gateway.core.BaseBizProcessor;
import com.sme.be_sme.shared.gateway.core.BizContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class PlatformFeedbackDetailProcessor extends BaseBizProcessor<BizContext> {

    private final ObjectMapper objectMapper;
    private final FeedbackMapper feedbackMapper;
    private final CompanyMapper companyMapper;
    private final UserMapper userMapper;

    @Override
    protected Object doProcess(BizContext context, JsonNode payload) {
        PlatformFeedbackDetailRequest request = objectMapper.convertValue(payload, PlatformFeedbackDetailRequest.class);

        if (!StringUtils.hasText(request.getFeedbackId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "feedbackId is required");
        }

        FeedbackEntity feedback = feedbackMapper.selectByPrimaryKey(request.getFeedbackId());
        if (feedback == null) {
            throw AppException.of(ErrorCodes.NOT_FOUND, "Feedback not found");
        }

        String companyName = null;
        if (StringUtils.hasText(feedback.getCompanyId())) {
            CompanyEntity company = companyMapper.selectByPrimaryKey(feedback.getCompanyId());
            if (company != null) {
                companyName = company.getName();
            }
        }

        String userName = null;
        if (StringUtils.hasText(feedback.getUserId())) {
            UserEntity user = userMapper.selectByPrimaryKey(feedback.getUserId());
            if (user != null) {
                userName = user.getFullName();
            }
        }

        PlatformFeedbackDetailResponse response = new PlatformFeedbackDetailResponse();
        response.setFeedbackId(feedback.getFeedbackId());
        response.setCompanyId(feedback.getCompanyId());
        response.setCompanyName(companyName);
        response.setUserId(feedback.getUserId());
        response.setUserName(userName);
        response.setSubject(feedback.getSubject());
        response.setContent(feedback.getContent());
        response.setStatus(feedback.getStatus());
        response.setResolvedAt(feedback.getResolvedAt());
        response.setResolvedBy(feedback.getResolvedBy());
        response.setCreatedAt(feedback.getCreatedAt());
        return response;
    }
}
