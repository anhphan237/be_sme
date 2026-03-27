package com.sme.be_sme.modules.survey.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.survey.api.request.SurveyInstanceGetRequest;
import com.sme.be_sme.modules.survey.api.response.SurveyInstanceGetResponse;
import com.sme.be_sme.modules.survey.infrastructure.mapper.SurveyAnswerMapperExt;
import com.sme.be_sme.modules.survey.infrastructure.mapper.SurveyInstanceMapperExt;
import com.sme.be_sme.modules.survey.infrastructure.mapper.SurveyResponseDraftMapperExt;
import com.sme.be_sme.modules.survey.infrastructure.mapper.SurveyResponseMapperExt;
import com.sme.be_sme.modules.survey.infrastructure.persistence.model.SurveyAnswerRow;
import com.sme.be_sme.modules.survey.infrastructure.persistence.model.SurveyInstanceDetailRow;
import com.sme.be_sme.modules.survey.infrastructure.persistence.model.SurveyResponseDraftRow;
import com.sme.be_sme.modules.survey.infrastructure.persistence.model.SurveyResponseRow;
import com.sme.be_sme.shared.constant.ErrorCodes;
import com.sme.be_sme.shared.exception.AppException;
import com.sme.be_sme.shared.gateway.core.BaseBizProcessor;
import com.sme.be_sme.shared.gateway.core.BizContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class SurveyInstanceGetProcessor extends BaseBizProcessor<BizContext> {

    private static final String ROLE_HR = "HR";
    private static final String STATUS_COMPLETED = "COMPLETED";

    private final ObjectMapper objectMapper;
    private final SurveyInstanceMapperExt surveyInstanceMapperExt;
    private final SurveyResponseDraftMapperExt surveyResponseDraftMapperExt;
    private final SurveyResponseMapperExt surveyResponseMapperExt;
    private final SurveyAnswerMapperExt surveyAnswerMapperExt;

    @Override
    protected Object doProcess(BizContext context, JsonNode payload) {
        SurveyInstanceGetRequest request = payload != null && !payload.isNull()
                ? objectMapper.convertValue(payload, SurveyInstanceGetRequest.class)
                : new SurveyInstanceGetRequest();

        validate(context, request);

        String companyId = context.getTenantId();
        String operatorId = context.getOperatorId();

        String responderUserId = shouldRestrictByResponder(context)
                ? operatorId
                : null;

        SurveyInstanceDetailRow row = surveyInstanceMapperExt.selectDetailById(
                companyId,
                request.getInstanceId(),
                responderUserId
        );

        if (row == null) {
            throw AppException.of(ErrorCodes.NOT_FOUND, "Survey instance not found");
        }
        if (!STATUS_COMPLETED.equalsIgnoreCase(row.getStatus())
                && row.getScheduledAt() != null
                && row.getScheduledAt().toInstant().isAfter(Instant.now())
                && shouldRestrictByResponder(context)) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "Survey is not open yet");
        }
        List<SurveyInstanceGetResponse.SurveyDraftAnswerDto> answers;
        if (STATUS_COMPLETED.equalsIgnoreCase(row.getStatus())) {
            answers = loadSubmittedAnswers(companyId, row.getSurveyInstanceId());
        } else {
            answers = loadDraftAnswers(companyId, row.getSurveyInstanceId(), operatorId);
        }

        SurveyInstanceGetResponse response = new SurveyInstanceGetResponse();
        response.setInstanceId(row.getSurveyInstanceId());
        response.setTemplateId(row.getSurveyTemplateId());
        response.setTemplateName(row.getTemplateName());
        response.setStatus(row.getStatus());
        response.setScheduledAt(
                row.getScheduledAt() != null ? row.getScheduledAt().toLocalDateTime() : null
        );
        response.setDraftAnswers(answers);
        return response;
    }

    private List<SurveyInstanceGetResponse.SurveyDraftAnswerDto> loadDraftAnswers(
            String companyId,
            String instanceId,
            String operatorId
    ) {
        List<SurveyResponseDraftRow> draftRows =
                surveyResponseDraftMapperExt.selectByInstanceIdAndResponder(
                        companyId,
                        instanceId,
                        operatorId
                );

        if (draftRows == null) {
            return new ArrayList<>();
        }

        return draftRows.stream()
                .map(this::toDraftAnswer)
                .collect(Collectors.toList());
    }

    private List<SurveyInstanceGetResponse.SurveyDraftAnswerDto> loadSubmittedAnswers(
            String companyId,
            String instanceId
    ) {
        SurveyResponseRow responseRow =
                surveyResponseMapperExt.selectLatestByInstanceId(companyId, instanceId);

        if (responseRow == null || !StringUtils.hasText(responseRow.getSurveyResponseId())) {
            return new ArrayList<>();
        }

        List<SurveyAnswerRow> answerRows =
                surveyAnswerMapperExt.selectByResponseId(companyId, responseRow.getSurveyResponseId());

        if (answerRows == null) {
            return new ArrayList<>();
        }

        return answerRows.stream()
                .map(this::toSubmittedAnswer)
                .collect(Collectors.toList());
    }

    private SurveyInstanceGetResponse.SurveyDraftAnswerDto toDraftAnswer(SurveyResponseDraftRow row) {
        SurveyInstanceGetResponse.SurveyDraftAnswerDto dto =
                new SurveyInstanceGetResponse.SurveyDraftAnswerDto();
        dto.setQuestionId(row.getQuestionId());

        try {
            dto.setValue(objectMapper.readValue(row.getAnswerValue(), Object.class));
        } catch (Exception e) {
            dto.setValue(row.getAnswerValue());
        }

        return dto;
    }

    private SurveyInstanceGetResponse.SurveyDraftAnswerDto toSubmittedAnswer(SurveyAnswerRow row) {
        SurveyInstanceGetResponse.SurveyDraftAnswerDto dto =
                new SurveyInstanceGetResponse.SurveyDraftAnswerDto();
        dto.setQuestionId(row.getSurveyQuestionId());

        Object value = null;

        if (row.getValueRating() != null) {
            value = row.getValueRating();
        } else if (StringUtils.hasText(row.getValueChoice())) {
            try {
                value = objectMapper.readValue(row.getValueChoice(), Object.class);
            } catch (Exception e) {
                value = row.getValueChoice();
            }
        } else if (StringUtils.hasText(row.getValueText())) {
            value = row.getValueText();
        }

        dto.setValue(value);
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

    private boolean shouldRestrictByResponder(BizContext context) {
        Set<String> roles = context.getRoles();
        if (CollectionUtils.isEmpty(roles)) {
            return true;
        }

        boolean isHr = roles.stream()
                .filter(StringUtils::hasText)
                .anyMatch(role -> ROLE_HR.equalsIgnoreCase(role));

        return !isHr;
    }
}