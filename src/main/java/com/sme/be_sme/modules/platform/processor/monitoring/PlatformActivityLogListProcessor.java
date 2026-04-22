package com.sme.be_sme.modules.platform.processor.monitoring;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.platform.api.request.PlatformActivityLogListRequest;
import com.sme.be_sme.modules.platform.api.response.PlatformActivityLogListResponse;
import com.sme.be_sme.modules.platform.api.response.PlatformActivityLogListResponse.ActivityLogItem;
import com.sme.be_sme.modules.platform.infrastructure.mapper.ActivityLogMapper;
import com.sme.be_sme.modules.platform.infrastructure.persistence.entity.ActivityLogEntity;
import com.sme.be_sme.shared.constant.ErrorCodes;
import com.sme.be_sme.shared.exception.AppException;
import com.sme.be_sme.shared.gateway.core.BaseBizProcessor;
import com.sme.be_sme.shared.gateway.core.BizContext;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class PlatformActivityLogListProcessor extends BaseBizProcessor<BizContext> {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;

    private final ObjectMapper objectMapper;
    private final ActivityLogMapper activityLogMapper;

    @Override
    protected Object doProcess(BizContext context, JsonNode payload) {
        PlatformActivityLogListRequest request = objectMapper.convertValue(payload, PlatformActivityLogListRequest.class);
        if (context == null || !StringUtils.hasText(context.getOperatorId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "operatorId is required");
        }

        int page = request.getPage() != null ? request.getPage() : DEFAULT_PAGE;
        int size = request.getSize() != null ? request.getSize() : DEFAULT_SIZE;
        if (page < 0) {
            page = DEFAULT_PAGE;
        }
        if (size <= 0) {
            size = DEFAULT_SIZE;
        }
        int offset = page * size;

        String userId = StringUtils.hasText(request.getUserId())
                ? request.getUserId().trim()
                : context.getOperatorId().trim();
        List<ActivityLogEntity> pageSlice;
        int total;
        pageSlice = activityLogMapper.selectByUserIdWithPaging(userId, offset, size);
        total = activityLogMapper.countByUserId(userId);

        List<ActivityLogItem> items = new ArrayList<>();
        for (ActivityLogEntity entity : pageSlice) {
            ActivityLogItem item = new ActivityLogItem();
            item.setLogId(entity.getLogId());
            item.setCompanyId(entity.getCompanyId());
            item.setUserId(entity.getUserId());
            item.setAction(entity.getAction());
            item.setEntityType(entity.getEntityType());
            item.setEntityId(entity.getEntityId());
            item.setDetail(entity.getDetail());
            item.setCreatedAt(entity.getCreatedAt());
            items.add(item);
        }

        PlatformActivityLogListResponse response = new PlatformActivityLogListResponse();
        response.setItems(items);
        response.setTotal(total);
        return response;
    }
}
