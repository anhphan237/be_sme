package com.sme.be_sme.modules.survey.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.identity.infrastructure.mapper.UserMapperExt;
import com.sme.be_sme.modules.notification.service.NotificationCreateParams;
import com.sme.be_sme.modules.notification.service.NotificationService;
import com.sme.be_sme.modules.survey.api.request.SurveySendRequest;
import com.sme.be_sme.modules.survey.api.response.SurveySendResponse;
import com.sme.be_sme.modules.survey.infrastructure.mapper.SurveyInstanceMapper;
import com.sme.be_sme.modules.survey.infrastructure.mapper.SurveyInstanceMapperExt;
import com.sme.be_sme.modules.survey.infrastructure.mapper.SurveyTemplateMapper;
import com.sme.be_sme.modules.survey.infrastructure.persistence.entity.SurveyInstanceEntity;
import com.sme.be_sme.modules.survey.infrastructure.persistence.entity.SurveyTemplateEntity;
import com.sme.be_sme.shared.constant.ErrorCodes;
import com.sme.be_sme.shared.exception.AppException;
import com.sme.be_sme.shared.gateway.core.BaseBizProcessor;
import com.sme.be_sme.shared.gateway.core.BizContext;
import com.sme.be_sme.shared.util.UuidGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class SurveyInstanceSendProcessor extends BaseBizProcessor<BizContext> {

    private static final String TEMPLATE_SURVEY_READY = "SURVEY_READY";
    private static final int DEFAULT_DUE_DAYS = 7;
    private static final int MAX_DUE_DAYS = 30;
    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final ObjectMapper objectMapper;
    private final SurveyInstanceMapper surveyInstanceMapper;
    private final SurveyTemplateMapper surveyTemplateMapper;
    private final NotificationService notificationService;
    private final UserMapperExt userMapperExt;
    private final SurveyInstanceMapperExt surveyInstanceMapperExt;

    @Override
    protected Object doProcess(BizContext context, JsonNode payload) {
        SurveySendRequest request = objectMapper.convertValue(payload, SurveySendRequest.class);
        validate(context, request);

        Date now = new Date();

        if (StringUtils.hasText(request.getSurveyInstanceId())) {
            return sendExistingInstance(context, request.getSurveyInstanceId().trim(), now);
        }

        SurveyTemplateEntity template = surveyTemplateMapper.selectByPrimaryKey(request.getTemplateId().trim());
        if (template == null || !context.getTenantId().equals(template.getCompanyId())) {
            throw AppException.of(ErrorCodes.NOT_FOUND, "survey template not found");
        }

        if (!"ACTIVE".equalsIgnoreCase(template.getStatus())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "only ACTIVE template can be sent");
        }

        String targetRole = StringUtils.hasText(request.getTargetRole())
                ? request.getTargetRole().trim()
                : template.getTargetRole();

        validateTargetRole(targetRole);

        String actualResponderUserId = resolveActualResponderUserId(
                request.getResponderUserId().trim(),
                targetRole
        );

        int dueDays = resolveDueDays(readDueDays(request));
        String createdInstanceId = createAndSend(
                context,
                template,
                request,
                actualResponderUserId,
                now,
                plusDaysEndOfDay(now, dueDays)
        );

        SurveySendResponse res = new SurveySendResponse();
        res.setSurveyInstanceId(createdInstanceId);
        res.setStatus("SENT");
        res.setSentAt(now);
        return res;
    }

    private SurveySendResponse sendExistingInstance(BizContext context, String surveyInstanceId, Date now) {
        SurveyInstanceEntity entity = surveyInstanceMapper.selectByPrimaryKey(surveyInstanceId);

        if (entity == null || !context.getTenantId().equals(entity.getCompanyId())) {
            throw AppException.of(ErrorCodes.NOT_FOUND, "survey instance not found");
        }

        if (!"SCHEDULED".equalsIgnoreCase(entity.getStatus())
                && !"PENDING".equalsIgnoreCase(entity.getStatus())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "survey instance cannot be sent");
        }

        SurveyTemplateEntity template = surveyTemplateMapper.selectByPrimaryKey(entity.getSurveyTemplateId());
        if (template == null || !context.getTenantId().equals(template.getCompanyId())) {
            throw AppException.of(ErrorCodes.NOT_FOUND, "survey template not found");
        }

        if (!"ACTIVE".equalsIgnoreCase(template.getStatus())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "only ACTIVE template can be sent");
        }

        if (entity.getClosedAt() != null && !entity.getClosedAt().after(now)) {
            entity.setStatus("EXPIRED");
            trySetUpdatedAt(entity, now);
            surveyInstanceMapper.updateByPrimaryKey(entity);
            throw AppException.of(ErrorCodes.BAD_REQUEST, "survey is expired");
        }

        if (entity.getClosedAt() == null) {
            entity.setClosedAt(plusDaysEndOfDay(now, DEFAULT_DUE_DAYS));
        }

        entity.setSentAt(now);
        entity.setStatus("SENT");
        trySetUpdatedAt(entity, now);

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

    private String createAndSend(
            BizContext context,
            SurveyTemplateEntity template,
            SurveySendRequest request,
            String responderUserId,
            Date now,
            Date closedAt
    ) {
        SurveyInstanceEntity existed = surveyInstanceMapperExt.findActiveByUniqueKey(
                context.getTenantId(),
                StringUtils.hasText(request.getOnboardingId()) ? request.getOnboardingId().trim() : null,
                template.getSurveyTemplateId(),
                responderUserId
        );

        if (existed != null) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "Survey already sent or scheduled for this user");
        }

        SurveyInstanceEntity entity = new SurveyInstanceEntity();
        entity.setSurveyInstanceId(UuidGenerator.generate());
        entity.setCompanyId(context.getTenantId());
        entity.setSurveyTemplateId(template.getSurveyTemplateId());
        entity.setOnboardingId(StringUtils.hasText(request.getOnboardingId()) ? request.getOnboardingId().trim() : null);
        entity.setResponderUserId(responderUserId);
        entity.setScheduledAt(now);
        entity.setSentAt(now);
        entity.setClosedAt(closedAt);
        entity.setStatus("SENT");
        entity.setCreatedAt(now);

        int inserted = surveyInstanceMapper.insert(entity);
        if (inserted != 1) {
            throw AppException.of(ErrorCodes.INTERNAL_ERROR, "create & send survey instance failed");
        }

        notifySurveyReady(entity);
        return entity.getSurveyInstanceId();
    }

    private String resolveActualResponderUserId(String responderUserId, String targetRole) {
        if ("EMPLOYEE".equalsIgnoreCase(targetRole)) {
            return responderUserId;
        }

        String managerId = userMapperExt.selectManagerUserIdByUserId(responderUserId);
        if (!StringUtils.hasText(managerId)) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "manager not found for employee");
        }

        return managerId;
    }

    private void notifySurveyReady(SurveyInstanceEntity entity) {
        if (entity == null || !StringUtils.hasText(entity.getResponderUserId())) {
            return;
        }

        String dueStr = entity.getClosedAt() != null
                ? entity.getClosedAt()
                .toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime()
                .format(DATE_FMT)
                : "";

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("dueDate", dueStr);

        NotificationCreateParams params = NotificationCreateParams.builder()
                .companyId(entity.getCompanyId())
                .userId(entity.getResponderUserId())
                .type("SURVEY_READY")
                .title("Survey ready")
                .content("A survey is ready for you to complete."
                        + (StringUtils.hasText(dueStr) ? " Please submit by " + dueStr + "." : ""))
                .refType("SURVEY")
                .refId(entity.getSurveyInstanceId())
                .sendEmail(true)
                .emailTemplate(TEMPLATE_SURVEY_READY)
                .emailPlaceholders(placeholders)
                .onboardingId(entity.getOnboardingId())
                .build();

        notificationService.create(params);
    }

    private static void validateTargetRole(String targetRole) {
        if (!"EMPLOYEE".equalsIgnoreCase(targetRole)
                && !"MANAGER".equalsIgnoreCase(targetRole)) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "invalid targetRole");
        }
    }

    private static Integer readDueDays(SurveySendRequest request) {
        try {
            Method method = request.getClass().getMethod("getDueDays");
            Object value = method.invoke(request);
            if (value == null) {
                return null;
            }
            if (value instanceof Number number) {
                return number.intValue();
            }
            return Integer.valueOf(String.valueOf(value));
        } catch (Exception ignored) {
            return null;
        }
    }

    private static int resolveDueDays(Integer dueDays) {
        if (dueDays == null) {
            return DEFAULT_DUE_DAYS;
        }

        if (dueDays < 1) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "dueDays must be >= 1");
        }

        if (dueDays > MAX_DUE_DAYS) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "dueDays must be <= " + MAX_DUE_DAYS);
        }

        return dueDays;
    }

    private static Date plusDaysEndOfDay(Date start, int days) {
        LocalDate date = start.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
                .plusDays(days);

        LocalDateTime endOfDay = LocalDateTime.of(date, LocalTime.of(23, 59, 59));

        return Date.from(endOfDay.atZone(ZoneId.systemDefault()).toInstant());
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

        if (!hasInstanceId && !StringUtils.hasText(request.getResponderUserId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "responderUserId is required");
        }
    }

    private void trySetUpdatedAt(SurveyInstanceEntity instance, Date now) {
        try {
            SurveyInstanceEntity.class
                    .getMethod("setUpdatedAt", Date.class)
                    .invoke(instance, now);
        } catch (Exception ignored) {
        }
    }
}