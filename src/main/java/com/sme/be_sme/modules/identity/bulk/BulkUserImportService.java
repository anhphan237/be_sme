package com.sme.be_sme.modules.identity.bulk;

import com.sme.be_sme.modules.identity.api.request.CreateUserRequest;
import com.sme.be_sme.modules.identity.api.response.CreateUserResponse;
import com.sme.be_sme.modules.identity.bulk.api.response.BulkUserImportCommitResponse;
import com.sme.be_sme.modules.identity.bulk.api.response.BulkUserImportValidateResponse;
import com.sme.be_sme.modules.identity.processor.IdentityUserCreateProcessor;
import com.sme.be_sme.modules.identity.service.UserService;
import com.sme.be_sme.shared.constant.ErrorCodes;
import com.sme.be_sme.shared.exception.AppException;
import com.sme.be_sme.shared.gateway.core.BizContext;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class BulkUserImportService {

    private static final Set<String> ALLOWED_ROLE_CODES = Set.of("ADMIN", "HR", "MANAGER", "IT", "EMPLOYEE");

    private final IdentityBulkUserImportProperties properties;
    private final BulkUserExcelService excelService;
    private final UserService userService;
    private final IdentityUserCreateProcessor identityUserCreateProcessor;

    public byte[] buildTemplateWorkbook() {
        ensureEnabled();
        return excelService.buildTemplateWorkbook();
    }

    public BulkUserImportValidateResponse validate(MultipartFile file, String companyId) {
        ensureEnabled();
        ValidationSnapshot snapshot = validateInternal(file, companyId);
        return snapshot.response();
    }

    public BulkUserImportCommitResponse commit(MultipartFile file, BizContext context) {
        ensureEnabled();
        ValidationSnapshot snapshot = validateInternal(file, context.getTenantId());

        BulkUserImportCommitResponse response = new BulkUserImportCommitResponse();
        response.setTotalRows(snapshot.rows().size());

        int createdCount = 0;
        int failedCount = 0;

        for (BulkUserExcelService.ParsedUserRow row : snapshot.rows()) {
            BulkUserImportCommitResponse.RowResult result = new BulkUserImportCommitResponse.RowResult();
            result.setRowNumber(row.getRowNumber());
            result.setEmail(row.getRequest() != null ? row.getRequest().getEmail() : null);
            result.setFullName(row.getRequest() != null ? row.getRequest().getFullName() : null);

            if (row.hasError()) {
                result.setStatus("FAILED_VALIDATION");
                result.getErrors().addAll(row.getErrors());
                failedCount++;
                response.getRows().add(result);
                continue;
            }

            try {
                CreateUserResponse created = identityUserCreateProcessor.process(context, row.getRequest());
                result.setStatus("CREATED");
                result.setUserId(created.getUserId());
                createdCount++;
            } catch (Exception ex) {
                result.setStatus("FAILED_CREATE");
                result.getErrors().add(safeErrorMessage(ex));
                failedCount++;
            }
            response.getRows().add(result);
        }

        response.setCreatedRows(createdCount);
        response.setFailedRows(failedCount);
        return response;
    }

    private ValidationSnapshot validateInternal(MultipartFile file, String companyId) {
        if (!StringUtils.hasText(companyId)) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "tenantId is required");
        }

        List<BulkUserExcelService.ParsedUserRow> rows = excelService.parseImportFile(file, properties);
        Map<String, Integer> firstSeenRowByEmail = new HashMap<>();

        for (BulkUserExcelService.ParsedUserRow row : rows) {
            CreateUserRequest request = row.getRequest();
            if (request == null) {
                row.addError("invalid row payload");
                continue;
            }

            validateRequiredFields(row, request);
            validateRoleAndDepartment(row, request);
            validateEmailDuplicateInFile(row, request, firstSeenRowByEmail);
            validateManagerIfPresent(row, request);
        }

        for (BulkUserExcelService.ParsedUserRow row : rows) {
            CreateUserRequest request = row.getRequest();
            if (request == null || row.hasError() || !StringUtils.hasText(request.getEmail())) {
                continue;
            }
            userService.findByLowerEmail(request.getEmail()).ifPresent(existing -> row.addError("email already exists in system"));
        }

        BulkUserImportValidateResponse response = new BulkUserImportValidateResponse();
        response.setTotalRows(rows.size());
        int valid = 0;
        int invalid = 0;
        for (BulkUserExcelService.ParsedUserRow row : rows) {
            BulkUserImportValidateResponse.RowResult result = new BulkUserImportValidateResponse.RowResult();
            result.setRowNumber(row.getRowNumber());
            result.setEmail(row.getRequest() != null ? row.getRequest().getEmail() : null);
            result.setFullName(row.getRequest() != null ? row.getRequest().getFullName() : null);
            result.setStatus(row.hasError() ? "INVALID" : "VALID");
            result.getErrors().addAll(row.getErrors());
            response.getRows().add(result);

            if (row.hasError()) {
                invalid++;
            } else {
                valid++;
            }
        }
        response.setValidRows(valid);
        response.setInvalidRows(invalid);

        return new ValidationSnapshot(rows, response);
    }

    private static void validateRequiredFields(BulkUserExcelService.ParsedUserRow row, CreateUserRequest request) {
        if (!StringUtils.hasText(request.getEmail())) {
            row.addError("email is required");
        }
        if (!StringUtils.hasText(request.getFullName())) {
            row.addError("fullName is required");
        }
        if (!StringUtils.hasText(request.getRoleCode())) {
            row.addError("roleCode is required");
        }
    }

    private static void validateRoleAndDepartment(BulkUserExcelService.ParsedUserRow row, CreateUserRequest request) {
        if (!StringUtils.hasText(request.getRoleCode())) {
            return;
        }

        String roleCode = request.getRoleCode().trim().toUpperCase(Locale.US);
        request.setRoleCode(roleCode);
        if (!ALLOWED_ROLE_CODES.contains(roleCode)) {
            row.addError("roleCode is invalid");
            return;
        }
        if (("MANAGER".equals(roleCode) || "EMPLOYEE".equals(roleCode))
                && !StringUtils.hasText(request.getDepartmentId())) {
            row.addError("departmentId is required for MANAGER/EMPLOYEE");
        }
    }

    private static void validateEmailDuplicateInFile(
            BulkUserExcelService.ParsedUserRow row,
            CreateUserRequest request,
            Map<String, Integer> firstSeenRowByEmail) {
        if (!StringUtils.hasText(request.getEmail())) {
            return;
        }
        String key = request.getEmail().trim().toLowerCase(Locale.US);
        Integer firstSeen = firstSeenRowByEmail.putIfAbsent(key, row.getRowNumber());
        if (firstSeen != null) {
            row.addError("duplicate email in file (already seen at row " + firstSeen + ")");
        }
    }

    private void validateManagerIfPresent(BulkUserExcelService.ParsedUserRow row, CreateUserRequest request) {
        if (!StringUtils.hasText(request.getManagerUserId())) {
            return;
        }
        userService.findByUserId(request.getManagerUserId())
                .orElseGet(() -> {
                    row.addError("managerUserId does not exist");
                    return null;
                });
    }

    private String safeErrorMessage(Exception ex) {
        if (ex instanceof AppException appException && StringUtils.hasText(appException.getMessage())) {
            return appException.getMessage();
        }
        if (StringUtils.hasText(ex.getMessage())) {
            return ex.getMessage();
        }
        return "failed to create user";
    }

    private void ensureEnabled() {
        if (!properties.isEnabled()) {
            throw AppException.of(ErrorCodes.FORBIDDEN, "bulk user import is disabled");
        }
    }

    private record ValidationSnapshot(
            List<BulkUserExcelService.ParsedUserRow> rows,
            BulkUserImportValidateResponse response) {}
}
