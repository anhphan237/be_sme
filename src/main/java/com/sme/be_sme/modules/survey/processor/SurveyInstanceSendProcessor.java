package com.sme.be_sme.modules.survey.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.identity.infrastructure.mapper.UserMapperExt;
import com.sme.be_sme.modules.survey.api.request.SurveySendRequest;
import com.sme.be_sme.modules.survey.api.response.SurveySendResponse;
import com.sme.be_sme.modules.survey.infrastructure.mapper.SurveyInstanceMapper;
import com.sme.be_sme.modules.survey.infrastructure.mapper.SurveyTemplateMapper;
import com.sme.be_sme.modules.survey.infrastructure.persistence.entity.SurveyInstanceEntity;
import com.sme.be_sme.modules.survey.infrastructure.persistence.entity.SurveyTemplateEntity;
import com.sme.be_sme.modules.notification.service.NotificationCreateParams;
import com.sme.be_sme.modules.notification.service.NotificationService;
import com.sme.be_sme.shared.constant.ErrorCodes;
import com.sme.be_sme.shared.exception.AppException;
import com.sme.be_sme.shared.gateway.core.BaseBizProcessor;
import com.sme.be_sme.shared.gateway.core.BizContext;
import com.sme.be_sme.shared.util.UuidGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class SurveyInstanceSendProcessor extends BaseBizProcessor<BizContext> {

    private static final String TEMPLATE_SURVEY_READY = "SURVEY_READY";
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ISO_LOCAL_DATE;

    private final ObjectMapper objectMapper;
    private final SurveyInstanceMapper surveyInstanceMapper;
    private final SurveyTemplateMapper surveyTemplateMapper;
    private final NotificationService notificationService;
    private final UserMapperExt userMapperExt;
    @Override
    protected Object doProcess(BizContext context, JsonNode payload) {
        SurveySendRequest request = objectMapper.convertValue(payload, SurveySendRequest.class);
        validate(context, request);

        Date now = new Date();

        if (StringUtils.hasText(request.getSurveyInstanceId())) {
            SurveyInstanceEntity entity = surveyInstanceMapper.selectByPrimaryKey(request.getSurveyInstanceId().trim());
            if (entity == null || !context.getTenantId().equals(entity.getCompanyId())) {
                throw AppException.of(ErrorCodes.NOT_FOUND, "survey instance not found");
            }

            entity.setSentAt(now);
            entity.setStatus("SENT");

            int updated = surveyInstanceMapper.updateByPrimaryKey(entity);
            if (updated != 1) {
                throw AppException.of(ErrorCodes.INTERNAL_ERROR, "send survey instance failed");
            }
            notifySurveyReady(entity);

            SurveySendResponse res = new SurveySendResponse();
            res.setSurveyInstanceId(entity.getSurveyInstanceId());
            res.setStatus(entity.getStatus());
            res.setSentAt(entity.getSentAt());
            return res;
        }

        SurveyTemplateEntity template = surveyTemplateMapper.selectByPrimaryKey(request.getTemplateId().trim());
        if (template == null || !context.getTenantId().equals(template.getCompanyId())) {
            throw AppException.of(ErrorCodes.NOT_FOUND, "survey template not found");
        }

        String targetRole = request.getTargetRole();
        String employeeId = request.getResponderUserId();

        String departmentId = userMapperExt.selectDepartmentIdByUserId(employeeId);
        String managerId = userMapperExt.selectManagerIdByDepartmentId(
                context.getTenantId(),
                departmentId
        );

        String createdInstanceId;

        if ("MANAGER".equals(targetRole)) {
            createdInstanceId = createAndSend(context, template, request, managerId, now);
        } else if ("BOTH".equals(targetRole)) {
            createAndSend(context, template, request, employeeId, now);
            createdInstanceId = createAndSend(context, template, request, managerId, now);
        } else {
            createdInstanceId = createAndSend(context, template, request, employeeId, now);
        }

        SurveySendResponse res = new SurveySendResponse();
        res.setSurveyInstanceId(createdInstanceId);
        res.setStatus("SENT");
        res.setSentAt(now);
        return res;
    }
    private String createAndSend(
            BizContext context,
            SurveyTemplateEntity template,
            SurveySendRequest request,
            String responderUserId,
            Date now
    ) {
        SurveyInstanceEntity entity = new SurveyInstanceEntity();
        entity.setSurveyInstanceId(UuidGenerator.generate());
        entity.setCompanyId(context.getTenantId());
        entity.setSurveyTemplateId(template.getSurveyTemplateId());
        entity.setOnboardingId(StringUtils.hasText(request.getOnboardingId()) ? request.getOnboardingId().trim() : null);
        entity.setResponderUserId(responderUserId);
        entity.setScheduledAt(now);
        entity.setSentAt(now);
        entity.setStatus("SENT");
        entity.setCreatedAt(now);

        int inserted = surveyInstanceMapper.insert(entity);
        if (inserted != 1) {
            throw AppException.of(ErrorCodes.INTERNAL_ERROR, "create & send survey instance failed");
        }

        notifySurveyReady(entity);
        return entity.getSurveyInstanceId();
    }
    private void notifySurveyReady(SurveyInstanceEntity entity) {
        if (!StringUtils.hasText(entity.getResponderUserId())) return;
        String dueStr = entity.getClosedAt() != null
                ? entity.getClosedAt().toInstant().atZone(ZoneId.systemDefault()).toLocalDate().format(DATE_FMT)
                : "";
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("dueDate", dueStr);
        NotificationCreateParams params = NotificationCreateParams.builder()
                .companyId(entity.getCompanyId())
                .userId(entity.getResponderUserId())
                .type("SURVEY_READY")
                .title("Survey ready")
                .content("A survey is ready for you to complete. Please submit by " + dueStr + ".")
                .refType("SURVEY")
                .refId(entity.getSurveyInstanceId())
                .sendEmail(true)
                .emailTemplate(TEMPLATE_SURVEY_READY)
                .emailPlaceholders(placeholders)
                .build();
        notificationService.create(params);
    }

    private static void validate(BizContext context, SurveySendRequest request) {
        if (context == null || !StringUtils.hasText(context.getTenantId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "tenantId is required");
        }
        if (request == null) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "payload is required");
        }
        boolean hasInstanceId = StringUtils.hasText(request.getSurveyInstanceId());
        boolean hasTemplateId = StringUtils.hasText(request.getTemplateId());

        if (!hasInstanceId && !hasTemplateId) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "surveyInstanceId or templateId is required");
        }
        if (!hasInstanceId) {
            if (!StringUtils.hasText(request.getResponderUserId())) {
                throw AppException.of(ErrorCodes.BAD_REQUEST, "responderUserId is required");
            }
        }
    }
}
