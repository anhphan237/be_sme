package com.sme.be_sme.modules.automation.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.automation.api.request.AutomationEmailSendRequest;
import com.sme.be_sme.modules.automation.api.response.AutomationEmailSendResponse;
import com.sme.be_sme.modules.automation.service.EmailSenderService;
import com.sme.be_sme.shared.constant.ErrorCodes;
import com.sme.be_sme.shared.exception.AppException;
import com.sme.be_sme.shared.gateway.core.BaseBizProcessor;
import com.sme.be_sme.shared.gateway.core.BizContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class AutomationEmailSendProcessor extends BaseBizProcessor<BizContext> {

    private final ObjectMapper objectMapper;
    private final EmailSenderService emailSenderService;

    @Override
    protected Object doProcess(BizContext context, JsonNode payload) {
        AutomationEmailSendRequest request = payload != null && !payload.isNull()
                ? objectMapper.convertValue(payload, AutomationEmailSendRequest.class)
                : new AutomationEmailSendRequest();
        validate(context, request);

        String companyId = context.getTenantId();
        Map<String, String> placeholders = request.getPlaceholders() != null ? request.getPlaceholders() : Collections.emptyMap();
        try {
            emailSenderService.sendWithTemplate(companyId, request.getTemplateCode().trim(), request.getToEmail().trim(),
                    placeholders, null, null);
        } catch (Exception e) {
            AutomationEmailSendResponse fail = new AutomationEmailSendResponse();
            fail.setSuccess(false);
            fail.setMessage(e.getMessage() != null ? e.getMessage() : "Send failed");
            return fail;
        }
        AutomationEmailSendResponse response = new AutomationEmailSendResponse();
        response.setSuccess(true);
        response.setMessage("Email sent");
        return response;
    }

    private static void validate(BizContext context, AutomationEmailSendRequest request) {
        if (context == null || !StringUtils.hasText(context.getTenantId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "tenantId is required");
        }
        if (request == null || !StringUtils.hasText(request.getTemplateCode())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "templateCode is required");
        }
        if (!StringUtils.hasText(request.getToEmail())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "toEmail is required");
        }
    }
}
