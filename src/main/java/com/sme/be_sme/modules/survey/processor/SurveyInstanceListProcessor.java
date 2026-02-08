package com.sme.be_sme.modules.survey.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.survey.api.request.SurveyInstanceListRequest;
import com.sme.be_sme.modules.survey.api.response.SurveyInstanceListResponse;
import com.sme.be_sme.modules.survey.infrastructure.mapper.SurveyInstanceMapperExt;
import com.sme.be_sme.modules.survey.infrastructure.persistence.model.SurveyInstanceListRow;
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
public class SurveyInstanceListProcessor extends BaseBizProcessor<BizContext> {

    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT = 100;

    private final ObjectMapper objectMapper;
    private final SurveyInstanceMapperExt surveyInstanceMapperExt;

    @Override
    protected Object doProcess(BizContext context, JsonNode payload) {
        SurveyInstanceListRequest request = payload != null && !payload.isNull()
                ? objectMapper.convertValue(payload, SurveyInstanceListRequest.class)
                : new SurveyInstanceListRequest();
        validate(context);

        String companyId = context.getTenantId();
        int limit = request.getLimit() != null && request.getLimit() > 0
                ? Math.min(request.getLimit(), MAX_LIMIT)
                : DEFAULT_LIMIT;
        int offset = request.getOffset() != null && request.getOffset() >= 0 ? request.getOffset() : 0;

        int totalCount = surveyInstanceMapperExt.countByCompanyId(
                companyId,
                request.getTemplateId(),
                request.getStatus(),
                request.getStartDate(),
                request.getEndDate()
        );
        List<SurveyInstanceListRow> rows = surveyInstanceMapperExt.selectListByCompanyId(
                companyId,
                request.getTemplateId(),
                request.getStatus(),
                request.getStartDate(),
                request.getEndDate(),
                offset,
                limit
        );
        if (rows == null) {
            rows = new ArrayList<>();
        }

        List<SurveyInstanceListResponse.SurveyInstanceItem> items = rows.stream()
                .map(this::toItem)
                .collect(Collectors.toList());

        SurveyInstanceListResponse response = new SurveyInstanceListResponse();
        response.setItems(items);
        response.setTotalCount(totalCount);
        return response;
    }

    private SurveyInstanceListResponse.SurveyInstanceItem toItem(SurveyInstanceListRow row) {
        SurveyInstanceListResponse.SurveyInstanceItem item = new SurveyInstanceListResponse.SurveyInstanceItem();
        item.setId(row.getSurveyInstanceId());
        item.setTemplateId(row.getSurveyTemplateId());
        item.setTemplateName(row.getTemplateName());
        item.setScheduledAt(row.getScheduledAt());
        item.setStatus(row.getStatus());
        item.setCreatedAt(row.getCreatedAt());
        return item;
    }

    private static void validate(BizContext context) {
        if (context == null || !StringUtils.hasText(context.getTenantId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "tenantId is required");
        }
    }
}
