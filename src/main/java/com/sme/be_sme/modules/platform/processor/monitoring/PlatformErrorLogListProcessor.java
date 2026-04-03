package com.sme.be_sme.modules.platform.processor.monitoring;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.platform.api.request.PlatformErrorLogListRequest;
import com.sme.be_sme.modules.platform.api.response.PlatformErrorLogListResponse;
import com.sme.be_sme.modules.platform.api.response.PlatformErrorLogListResponse.ErrorLogItem;
import com.sme.be_sme.modules.platform.infrastructure.mapper.ErrorLogMapper;
import com.sme.be_sme.modules.platform.infrastructure.persistence.entity.ErrorLogEntity;
import com.sme.be_sme.shared.gateway.core.BaseBizProcessor;
import com.sme.be_sme.shared.gateway.core.BizContext;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PlatformErrorLogListProcessor extends BaseBizProcessor<BizContext> {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;

    private final ObjectMapper objectMapper;
    private final ErrorLogMapper errorLogMapper;

    @Override
    protected Object doProcess(BizContext context, JsonNode payload) {
        PlatformErrorLogListRequest request = objectMapper.convertValue(payload, PlatformErrorLogListRequest.class);

        int page = request.getPage() != null ? request.getPage() : DEFAULT_PAGE;
        int size = request.getSize() != null ? request.getSize() : DEFAULT_SIZE;

        List<ErrorLogEntity> allLogs = errorLogMapper.selectAll();
        int total = allLogs.size();
        int fromIndex = Math.min(page * size, total);
        int toIndex = Math.min(fromIndex + size, total);
        List<ErrorLogEntity> pageSlice = allLogs.subList(fromIndex, toIndex);

        List<ErrorLogItem> items = new ArrayList<>();
        for (ErrorLogEntity entity : pageSlice) {
            ErrorLogItem item = new ErrorLogItem();
            item.setErrorId(entity.getErrorId());
            item.setErrorCode(entity.getErrorCode());
            item.setMessage(entity.getMessage());
            item.setRequestId(entity.getRequestId());
            item.setCreatedAt(entity.getCreatedAt());
            items.add(item);
        }

        PlatformErrorLogListResponse response = new PlatformErrorLogListResponse();
        response.setItems(items);
        response.setTotal(total);
        return response;
    }
}
