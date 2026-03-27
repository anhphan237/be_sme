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
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class SurveyInstanceListProcessor extends BaseBizProcessor<BizContext> {

    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT = 100;
    private static final String ROLE_EMPLOYEE = "EMPLOYEE";
    private static final String ROLE_MANAGER = "MANAGER";
    private static final String ROLE_HR = "HR";
    private final ObjectMapper objectMapper;
    private final SurveyInstanceMapperExt surveyInstanceMapperExt;

    @Override
    protected Object doProcess(BizContext context, JsonNode payload) {
        SurveyInstanceListRequest request = payload != null && !payload.isNull()
                ? objectMapper.convertValue(payload, SurveyInstanceListRequest.class)
                : new SurveyInstanceListRequest();

        validate(context);

        String companyId = context.getTenantId();
        String operatorId = context.getOperatorId();

        int limit = request.getLimit() != null && request.getLimit() > 0
                ? Math.min(request.getLimit(), MAX_LIMIT)
                : DEFAULT_LIMIT;
        int offset = request.getOffset() != null && request.getOffset() >= 0
                ? request.getOffset()
                : 0;

        String responderUserId = shouldRestrictByResponder(context) ? operatorId : null;

        int totalCount = surveyInstanceMapperExt.countByCompanyId(
                companyId,
                request.getTemplateId(),
                request.getStatus(),
                request.getStartDate(),
                request.getEndDate(),
                responderUserId
        );

        List<SurveyInstanceListRow> rows = surveyInstanceMapperExt.selectListByCompanyId(
                companyId,
                request.getTemplateId(),
                request.getStatus(),
                request.getStartDate(),
                request.getEndDate(),
                responderUserId,
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
    private boolean shouldRestrictByResponder(BizContext context) {
        Set<String> roles = context.getRoles();
        if (CollectionUtils.isEmpty(roles)) {
            return true;
        }

        boolean isHr = roles.stream()
                .filter(StringUtils::hasText)
                .anyMatch(role -> ROLE_HR.equalsIgnoreCase(role));

        if (isHr) {
            return false;
        }

        return roles.stream()
                .filter(StringUtils::hasText)
                .anyMatch(role ->
                        ROLE_EMPLOYEE.equalsIgnoreCase(role)
                                || ROLE_MANAGER.equalsIgnoreCase(role));
    }
    private SurveyInstanceListResponse.SurveyInstanceItem toItem(SurveyInstanceListRow row) {
        SurveyInstanceListResponse.SurveyInstanceItem item = new SurveyInstanceListResponse.SurveyInstanceItem();
        item.setId(row.getSurveyInstanceId());
        item.setTemplateId(row.getSurveyTemplateId());
        item.setTemplateName(row.getTemplateName());
        item.setScheduledAt(row.getScheduledAt());
        item.setStatus(row.getStatus());
        item.setCreatedAt(row.getCreatedAt());

        item.setResponderUserId(row.getResponderUserId());
        item.setInstanceId(row.getInstanceId());
        item.setEmployeeName(row.getEmployeeName());
        item.setEmail(row.getEmail());

        return item;
    }

    private static void validate(BizContext context) {
        if (context == null || !StringUtils.hasText(context.getTenantId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "tenantId is required");
        }
        if (context == null || !StringUtils.hasText(context.getOperatorId())) {
            throw AppException.of(ErrorCodes.UNAUTHORIZED, "operatorId is required");
        }
    }

}