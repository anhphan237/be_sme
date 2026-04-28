package com.sme.be_sme.modules.platform.processor.template;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.platform.api.request.ListPlatformTemplateRequest;
import com.sme.be_sme.modules.platform.api.response.ListPlatformTemplateResponse;
import com.sme.be_sme.modules.platform.api.response.PlatformTemplateListItemResponse;
import com.sme.be_sme.modules.platform.infrastructure.mapper.PlatformTemplateMapperExt;
import com.sme.be_sme.shared.gateway.core.BaseBizProcessor;
import com.sme.be_sme.shared.gateway.core.BizContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;

@Component
@RequiredArgsConstructor
public class PlatformListTemplateProcessor extends BaseBizProcessor<BizContext> {

    private final ObjectMapper objectMapper;
    private final PlatformTemplateMapperExt platformTemplateMapperExt;

    @Override
    protected Object doProcess(BizContext context, JsonNode payload) {
        PlatformTemplateBizHelper.assertPlatformAdmin(context, "list");

        ListPlatformTemplateRequest request =
                payload == null || payload.isNull()
                        ? new ListPlatformTemplateRequest()
                        : objectMapper.convertValue(payload, ListPlatformTemplateRequest.class);

        int page = normalizePage(request.getPage());
        int size = normalizeSize(request.getSize());
        int offset = (page - 1) * size;

        String keyword = normalizeKeyword(request.getKeyword());
        String status = normalizeStatus(request.getStatus());

        List<PlatformTemplateListItemResponse> items =
                platformTemplateMapperExt.selectPlatformTemplates(
                        PlatformTemplateBizHelper.PLATFORM_COMPANY_ID,
                        keyword,
                        status,
                        size,
                        offset
                );

        int total =
                platformTemplateMapperExt.countPlatformTemplates(
                        PlatformTemplateBizHelper.PLATFORM_COMPANY_ID,
                        keyword,
                        status
                );

        ListPlatformTemplateResponse response = new ListPlatformTemplateResponse();
        response.setItems(items);
        response.setTotal(total);
        response.setPage(page);
        response.setSize(size);
        return response;
    }

    private static int normalizePage(Integer page) {
        return page == null || page < 1 ? 1 : page;
    }

    private static int normalizeSize(Integer size) {
        if (size == null || size < 1) {
            return 20;
        }
        return Math.min(size, 100);
    }

    private static String normalizeKeyword(String keyword) {
        return StringUtils.hasText(keyword) ? keyword.trim() : null;
    }

    private static String normalizeStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return null;
        }

        String normalized = status.trim().toUpperCase(Locale.US);
        return "ALL".equals(normalized) ? null : normalized;
    }
}