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
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BulkUserImportServiceTest {

    @Mock
    private BulkUserExcelService excelService;
    @Mock
    private UserService userService;
    @Mock
    private IdentityUserCreateProcessor identityUserCreateProcessor;
    @Mock
    private MultipartFile file;

    private IdentityBulkUserImportProperties properties;
    private BulkUserImportService service;

    @BeforeEach
    void setUp() {
        properties = new IdentityBulkUserImportProperties();
        properties.setEnabled(true);
        service = new BulkUserImportService(properties, excelService, userService, identityUserCreateProcessor);
    }

    @Test
    void validate_marksInvalidRowsForDuplicateEmailAndRoleDepartmentRule() {
        BulkUserExcelService.ParsedUserRow row1 = row(2, "user1@acme.com", "User One", "EMPLOYEE", null);
        BulkUserExcelService.ParsedUserRow row2 = row(3, "user1@acme.com", "User Duplicate", "HR", null);

        when(excelService.parseImportFile(file, properties)).thenReturn(List.of(row1, row2));

        BulkUserImportValidateResponse response = service.validate(file, "company-1");

        assertEquals(2, response.getTotalRows());
        assertEquals(0, response.getValidRows());
        assertEquals(2, response.getInvalidRows());
        assertEquals("INVALID", response.getRows().get(0).getStatus());
        assertEquals("INVALID", response.getRows().get(1).getStatus());
        assertEquals("departmentId is required for MANAGER/EMPLOYEE", response.getRows().get(0).getErrors().get(0));
        assertEquals("duplicate email in file (already seen at row 2)", response.getRows().get(1).getErrors().get(0));
    }

    @Test
    void commit_returnsPartialSuccessWithoutStoppingWholeBatch() {
        BulkUserExcelService.ParsedUserRow validRow = row(2, "ok@acme.com", "Ok User", "HR", null);
        BulkUserExcelService.ParsedUserRow invalidRow = row(3, "bad@acme.com", "Bad User", "EMPLOYEE", null);
        BulkUserExcelService.ParsedUserRow createFailRow = row(4, "fail@acme.com", "Fail User", "HR", null);

        when(excelService.parseImportFile(file, properties)).thenReturn(List.of(validRow, invalidRow, createFailRow));
        when(userService.findByLowerEmail(any())).thenReturn(Optional.empty());

        CreateUserResponse created = new CreateUserResponse();
        created.setUserId("user-1");
        when(identityUserCreateProcessor.process(any(), any(CreateUserRequest.class)))
                .thenReturn(created)
                .thenThrow(AppException.of(ErrorCodes.DUPLICATED, "Email đã tồn tại trong hệ thống"));

        BizContext context = new BizContext();
        context.setTenantId("company-1");

        BulkUserImportCommitResponse response = service.commit(file, context);

        assertEquals(3, response.getTotalRows());
        assertEquals(1, response.getCreatedRows());
        assertEquals(2, response.getFailedRows());
        assertEquals("CREATED", response.getRows().get(0).getStatus());
        assertEquals("FAILED_VALIDATION", response.getRows().get(1).getStatus());
        assertEquals("FAILED_CREATE", response.getRows().get(2).getStatus());
    }

    @Test
    void validate_throwsForbiddenWhenFeatureDisabled() {
        properties.setEnabled(false);

        AppException ex = assertThrows(AppException.class, () -> service.validate(file, "company-1"));

        assertEquals(ErrorCodes.FORBIDDEN, ex.getCode());
    }

    private static BulkUserExcelService.ParsedUserRow row(
            int rowNumber,
            String email,
            String fullName,
            String roleCode,
            String departmentId) {
        BulkUserExcelService.ParsedUserRow row = new BulkUserExcelService.ParsedUserRow(rowNumber);
        CreateUserRequest request = new CreateUserRequest();
        request.setEmail(email);
        request.setFullName(fullName);
        request.setRoleCode(roleCode);
        request.setDepartmentId(departmentId);
        row.setRequest(request);
        return row;
    }
}
