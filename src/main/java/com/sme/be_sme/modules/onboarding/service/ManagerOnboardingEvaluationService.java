package com.sme.be_sme.modules.onboarding.service;

import com.sme.be_sme.modules.employee.infrastructure.mapper.EmployeeProfileMapper;
import com.sme.be_sme.modules.employee.infrastructure.persistence.entity.EmployeeProfileEntity;
import com.sme.be_sme.modules.notification.service.NotificationCreateParams;
import com.sme.be_sme.modules.notification.service.NotificationService;
import com.sme.be_sme.modules.onboarding.infrastructure.persistence.entity.OnboardingInstanceEntity;
import com.sme.be_sme.modules.survey.constant.SurveyPurpose;
import com.sme.be_sme.modules.survey.infrastructure.mapper.SurveyInstanceMapperExt;
import com.sme.be_sme.modules.survey.infrastructure.mapper.SurveyTemplateMapperExt;
import com.sme.be_sme.modules.survey.infrastructure.persistence.entity.SurveyInstanceEntity;
import com.sme.be_sme.modules.survey.infrastructure.persistence.model.ManagerEvaluationTemplateRow;
import com.sme.be_sme.modules.survey.service.ManagerEvaluationSendResult;
import com.sme.be_sme.shared.constant.ErrorCodes;
import com.sme.be_sme.shared.exception.AppException;
import com.sme.be_sme.shared.util.UuidGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ManagerOnboardingEvaluationService {

    private static final String STAGE_COMPLETED = "COMPLETED";
    private static final String TARGET_ROLE_MANAGER = "MANAGER";
    private static final String TEMPLATE_SURVEY_READY = "SURVEY_READY";
    private static final int DEFAULT_DUE_DAYS = 7;
    private static final int MAX_DUE_DAYS = 30;

    private static final String MSG_NO_MANAGER_EVALUATION_TEMPLATE =
            "Chưa có mẫu khảo sát đánh giá nhân viên sau khi hoàn thành onboarding. " +
                    "Vui lòng tạo một mẫu khảo sát loại “Đánh giá nhân viên sau onboarding” trước khi hoàn tất quy trình.";

    private static final String MSG_NO_DEFAULT_MANAGER_EVALUATION_TEMPLATE =
            "Đã có mẫu khảo sát đánh giá nhân viên sau onboarding, nhưng chưa có mẫu mặc định đang hoạt động. " +
                    "Vui lòng đặt một mẫu làm mặc định trước khi hoàn tất onboarding.";

    private static final String MSG_MANAGER_EVALUATION_TEMPLATE_NO_QUESTION =
            "Mẫu khảo sát đánh giá nhân viên sau onboarding chưa có câu hỏi. " +
                    "Vui lòng thêm ít nhất một câu hỏi trước khi hoàn tất onboarding.";

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final EmployeeProfileMapper employeeProfileMapper;
    private final SurveyTemplateMapperExt surveyTemplateMapperExt;
    private final SurveyInstanceMapperExt surveyInstanceMapperExt;
    private final NotificationService notificationService;

    public ManagerEvaluationSendResult sendAfterOnboardingCompleted(
            String companyId,
            String operatorId,
            OnboardingInstanceEntity onboardingInstance,
            String requestedTemplateId,
            Integer requestedDueDays
    ) {
        validateBase(companyId, onboardingInstance);

        EmployeeProfileEntity employeeProfile = resolveEmployeeProfile(onboardingInstance);

        String employeeUserId = resolveEmployeeUserId(employeeProfile);
        String managerUserId = resolveManagerUserId(onboardingInstance, employeeProfile);

        validateEmployeeAndManager(employeeUserId, managerUserId);

        ManagerEvaluationTemplateRow template = resolveManagerEvaluationTemplate(
                companyId,
                requestedTemplateId
        );

        validateManagerEvaluationTemplate(template);

        SurveyInstanceEntity existed = surveyInstanceMapperExt.findExistingManagerEvaluation(
                companyId,
                onboardingInstance.getOnboardingId(),
                template.getSurveyTemplateId(),
                managerUserId,
                employeeUserId
        );

        if (existed != null) {
            return ManagerEvaluationSendResult.skipped("manager evaluation survey already exists");
        }

        Date now = new Date();
        Date closedAt = plusDaysEndOfDay(now, resolveDueDays(requestedDueDays));
        String surveyInstanceId = UuidGenerator.generate();

        int inserted = surveyInstanceMapperExt.insertManagerEvaluationInstance(
                surveyInstanceId,
                companyId,
                onboardingInstance.getOnboardingId(),
                template.getSurveyTemplateId(),
                now,
                now,
                closedAt,
                "SENT",
                now,
                managerUserId,
                now,
                SurveyPurpose.MANAGER_EVALUATION,
                employeeUserId
        );

        if (inserted != 1) {
            throw AppException.of(ErrorCodes.INTERNAL_ERROR, "create manager evaluation survey failed");
        }

        notifyManagerEvaluationReady(
                companyId,
                managerUserId,
                onboardingInstance.getOnboardingId(),
                surveyInstanceId,
                closedAt
        );

        return ManagerEvaluationSendResult.sent(surveyInstanceId);
    }

    private static void validateBase(String companyId, OnboardingInstanceEntity onboardingInstance) {
        if (!StringUtils.hasText(companyId)) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "companyId is required");
        }

        if (onboardingInstance == null || !StringUtils.hasText(onboardingInstance.getOnboardingId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "onboarding instance is required");
        }

        if (!companyId.equals(onboardingInstance.getCompanyId())) {
            throw AppException.of(ErrorCodes.FORBIDDEN, "onboarding instance does not belong to tenant");
        }

        if (!StringUtils.hasText(onboardingInstance.getEmployeeId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "employee is required for onboarding instance");
        }
    }

    private EmployeeProfileEntity resolveEmployeeProfile(OnboardingInstanceEntity onboardingInstance) {
        EmployeeProfileEntity profile =
                employeeProfileMapper.selectByPrimaryKey(onboardingInstance.getEmployeeId().trim());

        if (profile == null) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "employee profile not found for onboarding instance");
        }

        if (!onboardingInstance.getCompanyId().equals(profile.getCompanyId())) {
            throw AppException.of(ErrorCodes.FORBIDDEN, "employee profile does not belong to tenant");
        }

        return profile;
    }

    private String resolveEmployeeUserId(EmployeeProfileEntity employeeProfile) {
        if (employeeProfile != null && StringUtils.hasText(employeeProfile.getUserId())) {
            return employeeProfile.getUserId().trim();
        }

        return null;
    }

    private String resolveManagerUserId(
            OnboardingInstanceEntity onboardingInstance,
            EmployeeProfileEntity employeeProfile
    ) {
        if (onboardingInstance != null && StringUtils.hasText(onboardingInstance.getManagerUserId())) {
            return onboardingInstance.getManagerUserId().trim();
        }

        if (employeeProfile != null && StringUtils.hasText(employeeProfile.getManagerUserId())) {
            return employeeProfile.getManagerUserId().trim();
        }

        return null;
    }

    private static void validateEmployeeAndManager(String employeeUserId, String managerUserId) {
        if (!StringUtils.hasText(employeeUserId)) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "employee user not found");
        }

        if (!StringUtils.hasText(managerUserId)) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "manager not found for employee");
        }

        if (employeeUserId.equals(managerUserId)) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "manager cannot be the same as employee");
        }
    }

    private ManagerEvaluationTemplateRow resolveManagerEvaluationTemplate(
            String companyId,
            String requestedTemplateId
    ) {
        if (StringUtils.hasText(requestedTemplateId)) {
            ManagerEvaluationTemplateRow template =
                    surveyTemplateMapperExt.selectManagerEvaluationTemplateById(
                            companyId,
                            requestedTemplateId.trim()
                    );

            if (template == null) {
                throw AppException.of(
                        ErrorCodes.BAD_REQUEST,
                        "Mẫu khảo sát đánh giá nhân viên sau onboarding không hợp lệ. " +
                                "Vui lòng chọn template có loại “Đánh giá nhân viên sau onboarding”, " +
                                "stage COMPLETED, targetRole MANAGER và trạng thái ACTIVE."
                );
            }

            return template;
        }

        int totalManagerEvaluationTemplates =
                surveyTemplateMapperExt.countManagerEvaluationTemplates(companyId);

        if (totalManagerEvaluationTemplates <= 0) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, MSG_NO_MANAGER_EVALUATION_TEMPLATE);
        }

        ManagerEvaluationTemplateRow defaultTemplate =
                surveyTemplateMapperExt.selectDefaultManagerEvaluationTemplate(companyId);

        if (defaultTemplate == null) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, MSG_NO_DEFAULT_MANAGER_EVALUATION_TEMPLATE);
        }

        return defaultTemplate;
    }

    private static void validateManagerEvaluationTemplate(ManagerEvaluationTemplateRow template) {
        if (template == null) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, MSG_NO_MANAGER_EVALUATION_TEMPLATE);
        }

        if (!SurveyPurpose.MANAGER_EVALUATION.equalsIgnoreCase(template.getPurpose())) {
            throw AppException.of(
                    ErrorCodes.BAD_REQUEST,
                    "Template đánh giá nhân viên sau onboarding phải có purpose = MANAGER_EVALUATION."
            );
        }

        if (!STAGE_COMPLETED.equalsIgnoreCase(template.getStage())) {
            throw AppException.of(
                    ErrorCodes.BAD_REQUEST,
                    "Template đánh giá nhân viên sau onboarding phải có stage = COMPLETED."
            );
        }

        if (!TARGET_ROLE_MANAGER.equalsIgnoreCase(template.getTargetRole())) {
            throw AppException.of(
                    ErrorCodes.BAD_REQUEST,
                    "Template đánh giá nhân viên sau onboarding phải có targetRole = MANAGER."
            );
        }

        if (!"ACTIVE".equalsIgnoreCase(template.getStatus())) {
            throw AppException.of(
                    ErrorCodes.BAD_REQUEST,
                    "Template đánh giá nhân viên sau onboarding phải ở trạng thái ACTIVE."
            );
        }

        if (template.getQuestionCount() == null || template.getQuestionCount() <= 0) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, MSG_MANAGER_EVALUATION_TEMPLATE_NO_QUESTION);
        }
    }

    private void notifyManagerEvaluationReady(
            String companyId,
            String managerUserId,
            String onboardingId,
            String surveyInstanceId,
            Date closedAt
    ) {
        String dueStr = closedAt != null
                ? closedAt.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime()
                .format(DATE_FMT)
                : "";

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("dueDate", dueStr);

        NotificationCreateParams params = NotificationCreateParams.builder()
                .companyId(companyId)
                .userId(managerUserId)
                .type("MANAGER_EVALUATION_READY")
                .title("Manager evaluation required")
                .content("Please evaluate the new employee after onboarding completion."
                        + (StringUtils.hasText(dueStr) ? " Please submit by " + dueStr + "." : ""))
                .refType("SURVEY")
                .refId(surveyInstanceId)
                .sendEmail(true)
                .emailTemplate(TEMPLATE_SURVEY_READY)
                .emailPlaceholders(placeholders)
                .onboardingId(onboardingId)
                .build();

        notificationService.create(params);
    }

    private static int resolveDueDays(Integer dueDays) {
        if (dueDays == null) {
            return DEFAULT_DUE_DAYS;
        }

        if (dueDays < 1) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "managerEvaluationDueDays must be >= 1");
        }

        if (dueDays > MAX_DUE_DAYS) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "managerEvaluationDueDays must be <= " + MAX_DUE_DAYS);
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
}