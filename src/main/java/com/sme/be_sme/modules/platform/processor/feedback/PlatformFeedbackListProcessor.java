package com.sme.be_sme.modules.platform.processor.feedback;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.company.infrastructure.mapper.CompanyMapper;
import com.sme.be_sme.modules.company.infrastructure.persistence.entity.CompanyEntity;
import com.sme.be_sme.modules.platform.api.request.PlatformFeedbackListRequest;
import com.sme.be_sme.modules.platform.api.response.PlatformFeedbackListResponse;
import com.sme.be_sme.modules.platform.api.response.PlatformFeedbackListResponse.FeedbackItem;
import com.sme.be_sme.modules.platform.infrastructure.mapper.FeedbackMapper;
import com.sme.be_sme.modules.platform.infrastructure.persistence.entity.FeedbackEntity;
import com.sme.be_sme.shared.gateway.core.BaseBizProcessor;
import com.sme.be_sme.shared.gateway.core.BizContext;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class PlatformFeedbackListProcessor extends BaseBizProcessor<BizContext> {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;

    private final ObjectMapper objectMapper;
    private final FeedbackMapper feedbackMapper;
    private final CompanyMapper companyMapper;

    @Override
    protected Object doProcess(BizContext context, JsonNode payload) {
        PlatformFeedbackListRequest request = objectMapper.convertValue(payload, PlatformFeedbackListRequest.class);

        int page = request.getPage() != null ? request.getPage() : DEFAULT_PAGE;
        int size = request.getSize() != null ? request.getSize() : DEFAULT_SIZE;

        List<FeedbackEntity> allFeedbacks = feedbackMapper.selectAll();

        List<FeedbackEntity> filtered = new ArrayList<>();
        for (FeedbackEntity fb : allFeedbacks) {
            if (fb == null) continue;
            if (StringUtils.hasText(request.getStatus())
                    && !request.getStatus().equalsIgnoreCase(fb.getStatus())) {
                continue;
            }
            filtered.add(fb);
        }

        Map<String, String> companyNameMap = buildCompanyNameMap();

        int total = filtered.size();
        int fromIndex = Math.min(page * size, total);
        int toIndex = Math.min(fromIndex + size, total);
        List<FeedbackEntity> pageSlice = filtered.subList(fromIndex, toIndex);

        List<FeedbackItem> items = new ArrayList<>();
        for (FeedbackEntity fb : pageSlice) {
            FeedbackItem item = new FeedbackItem();
            item.setFeedbackId(fb.getFeedbackId());
            item.setCompanyId(fb.getCompanyId());
            item.setCompanyName(companyNameMap.getOrDefault(fb.getCompanyId(), null));
            item.setUserId(fb.getUserId());
            item.setSubject(fb.getSubject());
            item.setStatus(fb.getStatus());
            item.setCreatedAt(fb.getCreatedAt());
            items.add(item);
        }

        PlatformFeedbackListResponse response = new PlatformFeedbackListResponse();
        response.setItems(items);
        response.setTotal(total);
        return response;
    }

    private Map<String, String> buildCompanyNameMap() {
        Map<String, String> map = new HashMap<>();
        for (CompanyEntity company : companyMapper.selectAll()) {
            if (company != null && company.getCompanyId() != null) {
                map.put(company.getCompanyId(), company.getName());
            }
        }
        return map;
    }
}
