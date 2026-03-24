package com.sme.be_sme.modules.survey.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.sme.be_sme.modules.survey.api.request.SurveyInstanceGetRequest;
import com.sme.be_sme.modules.survey.api.response.SurveyInstanceGetResponse;
import com.sme.be_sme.modules.survey.infrastructure.mapper.SurveyInstanceMapperExt;
import com.sme.be_sme.modules.survey.infrastructure.mapper.SurveyResponseDraftMapperExt;
import com.sme.be_sme.modules.survey.infrastructure.persistence.model.SurveyInstanceDetailRow;
import com.sme.be_sme.modules.survey.infrastructure.persistence.model.SurveyResponseDraftRow;
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
public class SurveyInstanceGetProcessor extends BaseBizProcessor<BizContext> {

    private static final String ROLE_EMPLOYEE = "EMPLOYEE";

    private final ObjectMapper objectMapper;
    private final SurveyInstanceMapperExt surveyInstanceMapperExt;
    private final SurveyResponseDraftMapperExt surveyResponseDraftMapperExt;

    @Override
    protected Object doProcess(BizContext context, JsonNode payload) {
        SurveyInstanceGetRequest request = payload != null && !payload.isNull()
                ? objectMapper.convertValue(payload, SurveyInstanceGetRequest.class)
                : new SurveyInstanceGetRequest();

        validate(context, request);

        String companyId = context.getTenantId();
        String operatorId = context.getOperatorId();
        String responderUserId = isEmployee(context) ? operatorId : null;

        SurveyInstanceDetailRow row = surveyInstanceMapperExt.selectDetailById(
                companyId,
                request.getInstanceId(),
                responderUserId
        );

        if (row == null) {
            throw AppException.of(ErrorCodes.NOT_FOUND, "Survey instance not found");
        }

        List<SurveyResponseDraftRow> draftRows =
                surveyResponseDraftMapperExt.selectByInstanceIdAndResponder(
                        companyId,
                        request.getInstanceId(),
                        operatorId
                );

        if (draftRows == null) {
            draftRows = new ArrayList<>();
        }

        List<SurveyInstanceGetResponse.SurveyDraftAnswerDto> draftAnswers = draftRows.stream()
                .map(this::toDraftAnswer)
                .collect(Collectors.toList());

        SurveyInstanceGetResponse response = new SurveyInstanceGetResponse();
        response.setInstanceId(row.getSurveyInstanceId());
        response.setTemplateId(row.getSurveyTemplateId());
        response.setTemplateName(row.getTemplateName());
        response.setStatus(row.getStatus());
        response.setScheduledAt(row.getScheduledAt());
        response.setDraftAnswers(draftAnswers);
        return response;
    }

    private SurveyInstanceGetResponse.SurveyDraftAnswerDto toDraftAnswer(SurveyResponseDraftRow row) {
        SurveyInstanceGetResponse.SurveyDraftAnswerDto dto = new SurveyInstanceGetResponse.SurveyDraftAnswerDto();
        dto.setQuestionId(row.getQuestionId());

        try {
            dto.setValue(objectMapper.readValue(row.getAnswerValue(), Object.class));
        } catch (Exception e) {
            dto.setValue(null);
        }

        return dto;
    }

    private static void validate(BizContext context, SurveyInstanceGetRequest request) {
        if (context == null || !StringUtils.hasText(context.getTenantId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "tenantId is required");
        }
        if (context == null || !StringUtils.hasText(context.getOperatorId())) {
            throw AppException.of(ErrorCodes.UNAUTHORIZED, "operatorId is required");
        }
        if (request == null || !StringUtils.hasText(request.getInstanceId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "instanceId is required");
        }
    }

    private boolean isEmployee(BizContext context) {
        Set<String> roles = context.getRoles();
        if (CollectionUtils.isEmpty(roles)) {
            return false;
        }

        return roles.stream()
                .filter(StringUtils::hasText)
                .anyMatch(role -> ROLE_EMPLOYEE.equalsIgnoreCase(role));
    }
}