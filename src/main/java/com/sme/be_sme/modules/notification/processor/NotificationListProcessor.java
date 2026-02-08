package com.sme.be_sme.modules.notification.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.notification.api.request.NotificationListRequest;
import com.sme.be_sme.modules.notification.api.response.NotificationListResponse;
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
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class NotificationListProcessor extends BaseBizProcessor<BizContext> {

    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT = 100;

    private final ObjectMapper objectMapper;
    private final NotificationMapper notificationMapper;

    @Override
    protected Object doProcess(BizContext context, JsonNode payload) {
        NotificationListRequest request = payload != null && !payload.isNull()
                ? objectMapper.convertValue(payload, NotificationListRequest.class)
                : new NotificationListRequest();
        validate(context);

        String companyId = context.getTenantId();
        String userId = context.getOperatorId();
        if (!StringUtils.hasText(userId)) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "operatorId is required");
        }

        Boolean unreadOnly = request.getUnreadOnly() != null ? request.getUnreadOnly() : null;
        int limit = request.getLimit() != null && request.getLimit() > 0
                ? Math.min(request.getLimit(), MAX_LIMIT)
                : DEFAULT_LIMIT;
        int offset = request.getOffset() != null && request.getOffset() >= 0 ? request.getOffset() : 0;

        int totalCount = notificationMapper.countByCompanyIdAndUserId(companyId, userId, unreadOnly);
        List<NotificationEntity> entities = notificationMapper.selectByCompanyIdAndUserId(companyId, userId, unreadOnly, offset, limit);
        if (entities == null) {
            entities = new ArrayList<>();
        }

        List<NotificationListResponse.NotificationItem> items = entities.stream()
                .map(this::toItem)
                .collect(Collectors.toList());

        NotificationListResponse response = new NotificationListResponse();
        response.setItems(items);
        response.setTotalCount(totalCount);
        return response;
    }

    private NotificationListResponse.NotificationItem toItem(NotificationEntity e) {
        NotificationListResponse.NotificationItem item = new NotificationListResponse.NotificationItem();
        item.setNotificationId(e.getNotificationId());
        item.setType(e.getType());
        item.setTitle(e.getTitle());
        item.setContent(e.getContent());
        item.setStatus(e.getStatus());
        item.setReadAt(e.getReadAt());
        item.setCreatedAt(e.getCreatedAt());
        item.setRefType(e.getRefType());
        item.setRefId(e.getRefId());
        return item;
    }

    private static void validate(BizContext context) {
        if (context == null || !StringUtils.hasText(context.getTenantId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "tenantId is required");
        }
    }
}
