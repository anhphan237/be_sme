package com.sme.be_sme.modules.survey.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.identity.infrastructure.mapper.UserMapperExt;
import com.sme.be_sme.modules.survey.api.request.SurveyScheduleRequest;
import com.sme.be_sme.modules.survey.api.response.SurveyScheduleResponse;
import com.sme.be_sme.modules.survey.infrastructure.mapper.SurveyInstanceMapper;
import com.sme.be_sme.modules.survey.infrastructure.mapper.SurveyTemplateMapper;
import com.sme.be_sme.modules.survey.infrastructure.persistence.entity.SurveyInstanceEntity;
import com.sme.be_sme.modules.survey.infrastructure.persistence.entity.SurveyTemplateEntity;

import com.sme.be_sme.modules.identity.infrastructure.mapper.UserMapper;
import com.sme.be_sme.modules.identity.infrastructure.persistence.entity.UserEntity;

import com.sme.be_sme.shared.constant.ErrorCodes;
import com.sme.be_sme.shared.exception.AppException;
import com.sme.be_sme.shared.gateway.core.BaseBizProcessor;
import com.sme.be_sme.shared.gateway.core.BizContext;
import com.sme.be_sme.shared.util.UuidGenerator;

import java.util.Date;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class SurveyScheduleProcessor extends BaseBizProcessor<BizContext> {

    private final ObjectMapper objectMapper;
    private final SurveyTemplateMapper surveyTemplateMapper;
    private final SurveyInstanceMapper surveyInstanceMapper;
    private final UserMapper userMapper;
    private final UserMapperExt userMapperExt;
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

        Date now = new Date();
        int dueDays = request.getDueDays() != null ? request.getDueDays() : 3;

        Date scheduledAt = request.getScheduledAt();
        Date closedAt = plusDays(scheduledAt, dueDays);

        String targetRole = request.getTargetRole();
        if (!StringUtils.hasText(targetRole)) {
            targetRole = template.getTargetRole();
        }
        if (!StringUtils.hasText(targetRole)) {
            targetRole = "EMPLOYEE";
        }

        String employeeId = request.getResponderUserId();
        String createdInstanceId = null;

        String departmentId = userMapperExt.selectDepartmentIdByUserId(employeeId);
        String managerId = null;
        if (StringUtils.hasText(departmentId)) {
            managerId = userMapperExt.selectManagerIdByDepartmentId(
                    context.getTenantId(),
                    departmentId
            );
        }

        if (("MANAGER".equalsIgnoreCase(targetRole) || "BOTH".equalsIgnoreCase(targetRole))
                && !StringUtils.hasText(managerId)) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "manager not found for department");
        }

        if ("MANAGER".equalsIgnoreCase(targetRole)) {
            createdInstanceId = createInstance(context, template, request, managerId, scheduledAt, closedAt);
        } else if ("BOTH".equalsIgnoreCase(targetRole)) {
            createInstance(context, template, request, employeeId, scheduledAt, closedAt);
            createdInstanceId = createInstance(context, template, request, managerId, scheduledAt, closedAt);
        } else {
            createdInstanceId = createInstance(context, template, request, employeeId, scheduledAt, closedAt);
        }

        UserEntity user = null;
        if (StringUtils.hasText(request.getResponderUserId())) {
            user = userMapper.selectByPrimaryKey(request.getResponderUserId());
        }

        SurveyScheduleResponse response = new SurveyScheduleResponse();
        response.setScheduleId(createdInstanceId);
        response.setStatus("SCHEDULED");
        response.setOpenAt(scheduledAt);
        response.setDueAt(closedAt);
        response.setTemplateId(template.getSurveyTemplateId());
        response.setResponderUserId(request.getResponderUserId());
        response.setInstanceId(createdInstanceId);

        if (user != null) {
            response.setEmployeeName(user.getFullName());
            response.setEmail(user.getEmail());
        }

        return response;
    }

    private String createInstance(
            BizContext context,
            SurveyTemplateEntity template,
            SurveyScheduleRequest request,
            String responderUserId,
            Date scheduledAt,
            Date closedAt
    ) {
        if (!StringUtils.hasText(responderUserId)) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "responderUserId is required");
        }

        SurveyInstanceEntity entity = new SurveyInstanceEntity();
        entity.setSurveyInstanceId(UuidGenerator.generate());
        entity.setCompanyId(context.getTenantId());
        entity.setOnboardingId(request.getOnboardingId());
        entity.setSurveyTemplateId(template.getSurveyTemplateId());
        entity.setScheduledAt(scheduledAt);
        entity.setClosedAt(closedAt);
        entity.setStatus("SCHEDULED");
        entity.setResponderUserId(responderUserId);
        entity.setCreatedAt(new Date());

        int inserted = surveyInstanceMapper.insert(entity);
        if (inserted != 1) {
            throw AppException.of(ErrorCodes.INTERNAL_ERROR, "schedule survey instance failed");
        }

        return entity.getSurveyInstanceId();
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