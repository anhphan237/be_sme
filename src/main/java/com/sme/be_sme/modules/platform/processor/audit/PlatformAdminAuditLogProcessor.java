package com.sme.be_sme.modules.platform.processor.audit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.platform.api.request.PlatformAdminAuditLogRequest;
import com.sme.be_sme.modules.platform.api.response.PlatformAdminAuditLogResponse;
import com.sme.be_sme.modules.platform.infrastructure.mapper.ActivityLogMapper;
import com.sme.be_sme.modules.platform.infrastructure.persistence.entity.ActivityLogEntity;
import com.sme.be_sme.shared.gateway.core.BaseBizProcessor;
import com.sme.be_sme.shared.gateway.core.BizContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class PlatformAdminAuditLogProcessor extends BaseBizProcessor<BizContext> {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;

    private final ObjectMapper objectMapper;
    private final ActivityLogMapper activityLogMapper;

    @Override
    protected Object doProcess(BizContext context, JsonNode payload) {
        PlatformAdminAuditLogRequest request =
                objectMapper.convertValue(payload, PlatformAdminAuditLogRequest.class);

        int page = request.getPage() != null ? request.getPage() : DEFAULT_PAGE;
        int size = request.getSize() != null ? request.getSize() : DEFAULT_SIZE;

        List<ActivityLogEntity> allLogs = activityLogMapper.selectAll();

        List<ActivityLogEntity> filtered = new ArrayList<>();
        for (ActivityLogEntity entity : allLogs) {
            if (entity == null) continue;
            if (request.getAdminUserId() != null && !request.getAdminUserId().equals(entity.getUserId())) continue;
            if (request.getAction() != null && !request.getAction().equalsIgnoreCase(entity.getAction())) continue;
            filtered.add(entity);
        }

        int total = filtered.size();
        int fromIndex = Math.min(page * size, total);
        int toIndex = Math.min(fromIndex + size, total);
        List<ActivityLogEntity> pageSlice = filtered.subList(fromIndex, toIndex);

        List<PlatformAdminAuditLogResponse.AdminAuditLogItem> items = new ArrayList<>();
        for (ActivityLogEntity entity : pageSlice) {
            PlatformAdminAuditLogResponse.AdminAuditLogItem item =
                    new PlatformAdminAuditLogResponse.AdminAuditLogItem();
            item.setLogId(entity.getLogId());
            item.setAdminUserId(entity.getUserId());
            item.setAction(entity.getAction());
            item.setTargetType(entity.getEntityType());
            item.setTargetId(entity.getEntityId());
            item.setDetail(entity.getDetail());
            item.setCreatedAt(entity.getCreatedAt());
            items.add(item);
        }

        PlatformAdminAuditLogResponse response = new PlatformAdminAuditLogResponse();
        response.setItems(items);
        response.setTotal(total);
        return response;
    }
}