package com.sme.be_sme.modules.identity.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.identity.api.request.CheckEmailRequest;
import com.sme.be_sme.modules.identity.api.response.CheckEmailResponse;
import com.sme.be_sme.modules.identity.service.UserService;
import com.sme.be_sme.shared.constant.ErrorCodes;
import com.sme.be_sme.shared.exception.AppException;
import com.sme.be_sme.shared.gateway.core.BaseBizProcessor;
import com.sme.be_sme.shared.gateway.core.BizContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CheckEmailExistsProcessor extends BaseBizProcessor<BizContext> {

    private final ObjectMapper objectMapper;
    private final UserService userService;

    @Override
    protected Object doProcess(BizContext context, JsonNode payload) {
        CheckEmailRequest request = objectMapper.convertValue(payload, CheckEmailRequest.class);
        if (request == null || request.getEmail() == null || request.getEmail().isBlank()) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "email is required");
        }
        String email = request.getEmail().trim().toLowerCase();
        boolean exists = userService.findByEmail(email).isPresent();

        CheckEmailResponse response = new CheckEmailResponse();
        response.setExists(exists);
        return response;
    }
}
