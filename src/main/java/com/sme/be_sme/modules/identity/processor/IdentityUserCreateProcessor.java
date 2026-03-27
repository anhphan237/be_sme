package com.sme.be_sme.modules.identity.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.automation.service.EmailSenderService;
import com.sme.be_sme.modules.company.infrastructure.mapper.CompanyMapper;
import com.sme.be_sme.modules.company.infrastructure.persistence.entity.CompanyEntity;
import com.sme.be_sme.modules.identity.invite.InviteTokenService;
import com.sme.be_sme.modules.employee.api.request.UpsertEmployeeProfileRequest;
import com.sme.be_sme.modules.employee.api.response.UpsertEmployeeProfileResponse;
import com.sme.be_sme.modules.employee.processor.UpsertEmployeeProfileProcessor;
import com.sme.be_sme.modules.identity.api.request.AssignRoleRequest;
import com.sme.be_sme.modules.identity.api.request.CreateUserRequest;
import com.sme.be_sme.modules.identity.api.response.CreateUserResponse;
import com.sme.be_sme.shared.constant.ErrorCodes;
import com.sme.be_sme.shared.exception.AppException;
import com.sme.be_sme.shared.gateway.core.BaseBizProcessor;
import com.sme.be_sme.shared.gateway.core.BizContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Gateway operation for: com.sme.identity.user.create
 *
 * Responsibilities (workflow 1):
 *  - create users
 *  - create/update employee_profiles
 *  - assign exactly one role (user_roles) via roleCode -> roleId
 *  - send invite email to new user with login credentials
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class IdentityUserCreateProcessor extends BaseBizProcessor<BizContext> {

    private static final String TEMPLATE_USER_INVITE = "USER_INVITE";
    private static final String TEMPLATE_USER_INVITE_LINK = "USER_INVITE_LINK";

    private final ObjectMapper objectMapper;
    private final CreateUserProcessor createUserProcessor;
    private final UpsertEmployeeProfileProcessor upsertEmployeeProfileProcessor;
    private final AssignRoleProcessor assignRoleProcessor;
    private final EmailSenderService emailSenderService;
    private final CompanyMapper companyMapper;
    private final InviteTokenService inviteTokenService;

    @Override
    protected Object doProcess(BizContext context, JsonNode payload) {
        CreateUserRequest req = objectMapper.convertValue(payload, CreateUserRequest.class);
        return process(context, req);
    }

    @Transactional
    public CreateUserResponse process(BizContext context, CreateUserRequest request) {
        validate(context, request);

        CreateUserResponse user = createUserProcessor.process(context, request);

        UpsertEmployeeProfileResponse profile = upsertEmployeeProfileProcessor.process(
                context,
                UpsertEmployeeProfileRequest.builder()
                        .userId(user.getUserId())
                        .departmentId(request.getDepartmentId())
                        .employeeCode(request.getEmployeeCode())
                        .employeeName(request.getFullName())
                        .employeeEmail(request.getEmail())
                        .employeePhone(request.getPhone())
                        .jobTitle(request.getJobTitle())
                        .managerUserId(request.getManagerUserId())
                        .startDate(request.getStartDate())
                        .workLocation(request.getWorkLocation())
                        .status(request.getStatus() == null ? "ACTIVE" : request.getStatus())
                        .build()
        );

        assignRoleProcessor.process(context, new AssignRoleRequest(user.getUserId(), request.getRoleCode()));

        // Send invite email to new user (non-blocking; failures logged but do not fail create)
        sendInviteEmail(context.getTenantId(), request, user.getUserId());

        // ✅ enrich response for FE
        user.setEmployeeId(profile.getEmployeeId());
        // optional echo
        user.setRoleCode(request.getRoleCode());

        return user;
    }

    private void sendInviteEmail(String companyId, CreateUserRequest request, String userId) {
        if (!StringUtils.hasText(request.getEmail())) return;
        try {
            String companyName = "";
            CompanyEntity company = companyMapper.selectByPrimaryKey(companyId);
            if (company != null && StringUtils.hasText(company.getName())) {
                companyName = company.getName();
            }
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("employeeName", StringUtils.hasText(request.getFullName()) ? request.getFullName() : "there");
            placeholders.put("companyName", companyName);
            placeholders.put("email", request.getEmail());

            boolean hasPassword = request.getPassword() != null && !request.getPassword().isBlank();
            if (hasPassword) {
                placeholders.put("password", request.getPassword());
                emailSenderService.sendWithTemplate(companyId, TEMPLATE_USER_INVITE,
                        request.getEmail(), placeholders, userId, null);
            } else {
                String rawToken = inviteTokenService.createToken(userId);
                String setPasswordLink = inviteTokenService.buildSetPasswordUrl(rawToken);
                placeholders.put("setPasswordLink", setPasswordLink);
                emailSenderService.sendWithTemplate(companyId, TEMPLATE_USER_INVITE_LINK,
                        request.getEmail(), placeholders, userId, null);
            }
        } catch (Exception e) {
            log.warn("Invite email failed for user {}: {}", userId, e.getMessage());
        }
    }

    private static void validate(BizContext context, CreateUserRequest request) {
        if (context == null || context.getTenantId() == null || context.getTenantId().isBlank()) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "tenantId is required");
        }
        if (request == null) throw AppException.of(ErrorCodes.BAD_REQUEST, "payload is required");
        if (isBlank(request.getEmail())) throw AppException.of(ErrorCodes.BAD_REQUEST, "email is required");
        if (isBlank(request.getFullName())) throw AppException.of(ErrorCodes.BAD_REQUEST, "fullName is required");
        if (isBlank(request.getRoleCode())) throw AppException.of(ErrorCodes.BAD_REQUEST, "roleCode is required");
        String roleUpper = request.getRoleCode().trim().toUpperCase(Locale.ROOT);
        if (("EMPLOYEE".equals(roleUpper) || "MANAGER".equals(roleUpper)) && isBlank(request.getDepartmentId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "departmentId is required for EMPLOYEE and MANAGER");
        }
    }

    private static boolean isBlank(String s) { return s == null || s.isBlank(); }
}

