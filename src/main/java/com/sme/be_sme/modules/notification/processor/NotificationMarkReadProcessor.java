package com.sme.be_sme.modules.notification.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.notification.api.request.NotificationMarkReadRequest;
import com.sme.be_sme.modules.notification.api.response.NotificationMarkReadResponse;
import com.sme.be_sme.modules.notification.infrastructure.mapper.NotificationMapper;
import com.sme.be_sme.modules.notification.infrastructure.persistence.entity.NotificationEntity;
import com.sme.be_sme.shared.constant.ErrorCodes;
import com.sme.be_sme.shared.exception.AppException;
import com.sme.be_sme.shared.gateway.core.BaseBizProcessor;
import com.sme.be_sme.shared.gateway.core.BizContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class NotificationMarkReadProcessor extends BaseBizProcessor<BizContext> {

    private final ObjectMapper objectMapper;
    private final NotificationMapper notificationMapper;

    @Override
    protected Object doProcess(BizContext context, JsonNode payload) {
        NotificationMarkReadRequest request = objectMapper.convertValue(payload, NotificationMarkReadRequest.class);
        validate(context, request);

        String companyId = context.getTenantId();
        String userId = context.getOperatorId();
        List<String> ids = request.getNotificationIds() != null
                ? request.getNotificationIds().stream().filter(StringUtils::hasText).distinct().collect(Collectors.toList())
                : new ArrayList<>();

        if (ids.isEmpty()) {
            NotificationMarkReadResponse response = new NotificationMarkReadResponse();
            response.setMarkedCount(0);
            return response;
        }

        List<String> allowedIds = new ArrayList<>();
        for (String id : ids) {
            NotificationEntity n = notificationMapper.selectByPrimaryKey(id);
            if (n != null && companyId.equals(n.getCompanyId()) && userId.equals(n.getUserId())) {
                allowedIds.add(id);
            }
        }

        Date now = new Date();
        int updated = allowedIds.isEmpty() ? 0 : notificationMapper.updateReadAtByIds(allowedIds, now);

        NotificationMarkReadResponse response = new NotificationMarkReadResponse();
        response.setMarkedCount(updated);
        return response;
    }

    private static void validate(BizContext context, NotificationMarkReadRequest request) {
        if (context == null || !StringUtils.hasText(context.getTenantId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "tenantId is required");
        }
        if (context == null || !StringUtils.hasText(context.getOperatorId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "operatorId is required");
        }
        if (request == null) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "payload is required");
        }
    }
}
