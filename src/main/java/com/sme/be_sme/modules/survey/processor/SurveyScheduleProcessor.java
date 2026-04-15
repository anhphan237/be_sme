package com.sme.be_sme.modules.survey.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.identity.infrastructure.mapper.UserMapper;
import com.sme.be_sme.modules.identity.infrastructure.mapper.UserMapperExt;
import com.sme.be_sme.modules.identity.infrastructure.persistence.entity.UserEntity;
import com.sme.be_sme.modules.notification.service.NotificationCreateParams;
import com.sme.be_sme.modules.notification.service.NotificationService;
import com.sme.be_sme.modules.survey.api.request.SurveyScheduleRequest;
import com.sme.be_sme.modules.survey.api.response.SurveyScheduleResponse;
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

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class SurveyScheduleProcessor extends BaseBizProcessor<BizContext> {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final ObjectMapper objectMapper;
    private final SurveyTemplateMapper surveyTemplateMapper;
    private final SurveyInstanceMapper surveyInstanceMapper;
    private final SurveyInstanceMapperExt surveyInstanceMapperExt;
    private final UserMapper userMapper;
    private final UserMapperExt userMapperExt;
    private final NotificationService notificationService;

    @Override
    protected Object doProcess(BizContext context, JsonNode payload) {
        SurveyScheduleRequest request =
                objectMapper.convertValue(payload, SurveyScheduleRequest.class);
        validate(context, request);

        SurveyTemplateEntity template =
                surveyTemplateMapper.selectByPrimaryKey(request.getTemplateId().trim());

        if (template == null || !context.getTenantId().equals(template.getCompanyId())) {
            throw AppException.of(ErrorCodes.NOT_FOUND, "survey template not found");
        }
        if (!"ACTIVE".equalsIgnoreCase(template.getStatus())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "only ACTIVE template can be scheduled");
        }

        Date now = new Date();
        Date requestedScheduledAt = request.getScheduledAt();

        LocalDate today = now.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDate();

        LocalDate scheduledDate = requestedScheduledAt.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDate();

        if (scheduledDate.isBefore(today)) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "scheduledAt cannot be in the past");
        }

        Date scheduledAt = scheduledDate.isEqual(today) ? now : requestedScheduledAt;

        int dueDays = request.getDueDays() != null ? request.getDueDays() : 3;
        Date closedAt = plusDays(scheduledAt, dueDays);

        String targetRole = StringUtils.hasText(request.getTargetRole())
                ? request.getTargetRole().trim()
                : template.getTargetRole();

        if (!"EMPLOYEE".equalsIgnoreCase(targetRole) && !"MANAGER".equalsIgnoreCase(targetRole)) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "invalid targetRole");
        }

        String employeeId = request.getResponderUserId().trim();
        String actualResponderUserId = request.getResponderUserId().trim();

        if ("MANAGER".equalsIgnoreCase(targetRole)) {
            String managerId = userMapperExt.selectManagerUserIdByUserId(actualResponderUserId);
            if (StringUtils.hasText(managerId)) {
                actualResponderUserId = managerId;
            }
        }

        boolean sendNow = !scheduledAt.after(now);

        String createdInstanceId = createInstance(
                context,
                template,
                request,
                actualResponderUserId,
                scheduledAt,
                closedAt,
                sendNow,
                now
        );

        UserEntity actualUser = userMapper.selectByPrimaryKey(actualResponderUserId);

        SurveyScheduleResponse response = new SurveyScheduleResponse();
        response.setScheduleId(createdInstanceId);
        response.setStatus(sendNow ? "SENT" : "SCHEDULED");
        response.setOpenAt(scheduledAt);
        response.setDueAt(closedAt);
        response.setTemplateId(template.getSurveyTemplateId());
        response.setResponderUserId(actualResponderUserId);
        response.setInstanceId(createdInstanceId);

        if (actualUser != null) {
            response.setEmployeeName(actualUser.getFullName());
            response.setEmail(actualUser.getEmail());
        }

        return response;
    }

    private String createInstance(
            BizContext context,
            SurveyTemplateEntity template,
            SurveyScheduleRequest request,
            String responderUserId,
            Date scheduledAt,
            Date closedAt,
            boolean sendNow,
            Date now
    ) {
        if (!StringUtils.hasText(responderUserId)) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "responderUserId is required");
        }

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
        entity.setOnboardingId(StringUtils.hasText(request.getOnboardingId()) ? request.getOnboardingId().trim() : null);
        entity.setSurveyTemplateId(template.getSurveyTemplateId());
        entity.setResponderUserId(responderUserId);
        entity.setScheduledAt(scheduledAt);
        entity.setClosedAt(closedAt);
        entity.setCreatedAt(now);

        if (sendNow) {
            entity.setStatus("SENT");
            entity.setSentAt(now);
        } else {
            entity.setStatus("SCHEDULED");
        }

        int inserted = surveyInstanceMapper.insert(entity);
        if (inserted != 1) {
            throw AppException.of(ErrorCodes.INTERNAL_ERROR, "schedule survey instance failed");
        }

        if (sendNow) {
            notifySurveyReady(entity);
        }

        return entity.getSurveyInstanceId();
    }

    private void notifySurveyReady(SurveyInstanceEntity entity) {
        if (entity == null || !StringUtils.hasText(entity.getResponderUserId())) {
            return;
        }

        String dueStr = "";
        if (entity.getClosedAt() != null) {
            dueStr = entity.getClosedAt().toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime()
                    .format(DATE_FMT);
        }

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("dueDate", dueStr);

        NotificationCreateParams params = NotificationCreateParams.builder()
                .companyId(entity.getCompanyId())
                .userId(entity.getResponderUserId())
                .type("SURVEY_READY")
                .title("New onboarding survey")
                .content("You have a new survey to complete"
                        + (StringUtils.hasText(dueStr) ? ". Please submit before " + dueStr : "."))
                .refType("SURVEY")
                .refId(entity.getSurveyInstanceId())
                .sendEmail(true)
                .emailTemplate("SURVEY_READY")
                .emailPlaceholders(placeholders)
                .onboardingId(entity.getOnboardingId())
                .build();

        notificationService.create(params);
    }

    private static Date plusDays(Date start, int days) {
        return new Date(start.getTime() + (long) days * 24 * 60 * 60 * 1000);
    }

    private static void validate(BizContext context, SurveyScheduleRequest request) {
        if (context == null || !StringUtils.hasText(context.getTenantId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "tenantId is required");
        }
        if (request == null) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "payload is required");
        }
        if (!StringUtils.hasText(request.getTemplateId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "templateId is required");
        }
        if (!StringUtils.hasText(request.getOnboardingId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "onboardingId is required");
        }
        if (!StringUtils.hasText(request.getResponderUserId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "responderUserId is required");
        }
        if (request.getScheduledAt() == null) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "scheduledAt is required");
        }
        if (request.getDueDays() != null && request.getDueDays() < 0) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "dueDays must be >= 0");
        }
    }
}