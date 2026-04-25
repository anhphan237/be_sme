package com.sme.be_sme.modules.onboarding.api;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sme.be_sme.modules.company.infrastructure.mapper.DepartmentTypeMapper;
import com.sme.be_sme.modules.company.infrastructure.persistence.entity.DepartmentTypeEntity;
import com.sme.be_sme.modules.onboarding.api.request.ChecklistTemplateCreateItem;
import com.sme.be_sme.modules.onboarding.api.request.ChecklistTemplateUpdateItem;
import com.sme.be_sme.modules.onboarding.api.request.OnboardingTemplateCreateRequest;
import com.sme.be_sme.modules.onboarding.api.request.OnboardingTemplateGetRequest;
import com.sme.be_sme.modules.onboarding.api.request.OnboardingTemplateUpdateRequest;
import com.sme.be_sme.modules.onboarding.api.request.TaskTemplateCreateItem;
import com.sme.be_sme.modules.onboarding.api.request.TaskTemplateUpdateItem;
import com.sme.be_sme.modules.onboarding.api.response.OnboardingTemplateGetResponse;
import com.sme.be_sme.modules.onboarding.api.response.OnboardingTemplateResponse;
import com.sme.be_sme.modules.onboarding.api.response.TaskLibraryImportResponse;
import com.sme.be_sme.modules.onboarding.api.response.TaskLibraryListResponse;
import com.sme.be_sme.modules.onboarding.facade.OnboardingTemplateFacade;
import com.sme.be_sme.modules.onboarding.infrastructure.mapper.OnboardingTemplateMapperExt;
import com.sme.be_sme.modules.onboarding.infrastructure.persistence.entity.OnboardingTemplateEntity;
import com.sme.be_sme.modules.onboarding.service.TaskLibraryExcelService;
import com.sme.be_sme.shared.api.BaseResponse;
import com.sme.be_sme.shared.constant.ErrorCodes;
import com.sme.be_sme.shared.exception.AppException;
import com.sme.be_sme.shared.gateway.core.BizContext;
import com.sme.be_sme.shared.gateway.core.BizContextHolder;
import com.sme.be_sme.shared.security.GatewayAuthGuard;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/task-libraries")
@RequiredArgsConstructor
public class TaskLibraryController {

    private static final String OP_LIST = "com.sme.onboarding.taskLibrary.list";
    private static final String OP_GET = "com.sme.onboarding.taskLibrary.get";
    private static final String OP_DOWNLOAD_TEMPLATE = "com.sme.onboarding.taskLibrary.downloadExcelTemplate";
    private static final String OP_IMPORT = "com.sme.onboarding.taskLibrary.importExcel";

    private final GatewayAuthGuard authGuard;
    private final OnboardingTemplateMapperExt onboardingTemplateMapperExt;
    private final DepartmentTypeMapper departmentTypeMapper;
    private final OnboardingTemplateFacade onboardingTemplateFacade;
    private final TaskLibraryExcelService taskLibraryExcelService;

    @GetMapping
    public BaseResponse<TaskLibraryListResponse> list(
            @RequestParam(value = "status", required = false) String status,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        String requestId = UUID.randomUUID().toString();
        BizContext ctx = authGuard.buildContext(OP_LIST, requestId, new ObjectNode(JsonNodeFactory.instance), authorization);

        String normalizedStatus = normalizeStatus(status);
        List<OnboardingTemplateEntity> rows =
                onboardingTemplateMapperExt.selectTaskLibrariesByCompanyIdAndStatus(ctx.getTenantId(), normalizedStatus);
        Map<String, String> departmentTypeNameByCode = departmentTypeMapper
                .selectByCompany(ctx.getTenantId())
                .stream()
                .collect(Collectors.toMap(
                        e -> e.getCode().trim().toUpperCase(Locale.US),
                        DepartmentTypeEntity::getName,
                        (a, b) -> a));

        TaskLibraryListResponse response = new TaskLibraryListResponse();
        response.setItems(rows.stream().map(row -> {
            TaskLibraryListResponse.TaskLibraryItem item = new TaskLibraryListResponse.TaskLibraryItem();
            item.setTemplateId(row.getOnboardingTemplateId());
            item.setName(row.getName());
            item.setStatus(row.getStatus());
            item.setDepartmentTypeCode(row.getDepartmentTypeCode());
            String key = row.getDepartmentTypeCode() == null ? null : row.getDepartmentTypeCode().trim().toUpperCase(Locale.US);
            item.setDepartmentTypeName(key == null ? null : departmentTypeNameByCode.get(key));
            return item;
        }).toList());
        return BaseResponse.success(requestId, response);
    }

    @GetMapping("/{templateId}")
    public BaseResponse<OnboardingTemplateGetResponse> get(
            @PathVariable("templateId") String templateId,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        String requestId = UUID.randomUUID().toString();
        BizContext ctx = authGuard.buildContext(OP_GET, requestId, new ObjectNode(JsonNodeFactory.instance), authorization);

        OnboardingTemplateEntity entity =
                onboardingTemplateMapperExt.selectTaskLibraryByIdAndCompany(templateId, ctx.getTenantId());
        if (entity == null) {
            throw AppException.of(ErrorCodes.NOT_FOUND, "task library not found");
        }

        OnboardingTemplateGetRequest request = new OnboardingTemplateGetRequest();
        request.setTemplateId(templateId);
        BizContextHolder.set(ctx);
        try {
            OnboardingTemplateGetResponse response = onboardingTemplateFacade.getOnboardingTemplate(request);
            return BaseResponse.success(requestId, response);
        } finally {
            BizContextHolder.clear();
        }
    }

    @GetMapping("/excel-template")
    public ResponseEntity<byte[]> downloadTemplate(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        String requestId = UUID.randomUUID().toString();
        authGuard.buildContext(OP_DOWNLOAD_TEMPLATE, requestId, new ObjectNode(JsonNodeFactory.instance), authorization);

        byte[] bytes = taskLibraryExcelService.buildTemplateWorkbook();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(
                MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.setContentDisposition(
                ContentDisposition.attachment().filename("task-library-template.xlsx").build());
        headers.set("X-Request-Id", requestId);
        return ResponseEntity.ok().headers(headers).body(bytes);
    }

    @PostMapping(value = "/import-excel", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public BaseResponse<TaskLibraryImportResponse> importExcel(
            @RequestParam("file") MultipartFile file,
            @RequestParam("departmentTypeCode") String departmentTypeCode,
            @RequestParam(value = "templateName", required = false) String templateName,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "replaceExisting", required = false, defaultValue = "true") boolean replaceExisting,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        String requestId = UUID.randomUUID().toString();
        BizContext ctx = authGuard.buildContext(OP_IMPORT, requestId, new ObjectNode(JsonNodeFactory.instance), authorization);

        if (!StringUtils.hasText(departmentTypeCode)) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "departmentTypeCode is required");
        }
        String normalizedDepartmentTypeCode = departmentTypeCode.trim().toUpperCase(Locale.US);
        assertDepartmentTypeActive(ctx.getTenantId(), normalizedDepartmentTypeCode);

        TaskLibraryExcelService.ParsedTaskLibrary parsed = taskLibraryExcelService.parseImportFile(file);
        OnboardingTemplateEntity existing = onboardingTemplateMapperExt.selectTaskLibraryByDepartmentTypeCode(
                ctx.getTenantId(), normalizedDepartmentTypeCode);

        OnboardingTemplateResponse upsertResponse;
        boolean created;
        BizContextHolder.set(ctx);
        try {
            if (existing != null) {
                if (!replaceExisting) {
                    throw AppException.of(
                            ErrorCodes.DUPLICATED,
                            "task library already exists for departmentTypeCode=" + normalizedDepartmentTypeCode);
                }
                OnboardingTemplateUpdateRequest updateRequest = new OnboardingTemplateUpdateRequest();
                updateRequest.setTemplateId(existing.getOnboardingTemplateId());
                updateRequest.setName(StringUtils.hasText(templateName) ? templateName.trim() : existing.getName());
                updateRequest.setDescription(description);
                updateRequest.setStatus("ACTIVE");
                updateRequest.setChecklists(toChecklistUpdates(parsed.getChecklists()));
                upsertResponse = onboardingTemplateFacade.updateOnboardingTemplate(updateRequest);
                created = false;
            } else {
                OnboardingTemplateCreateRequest createRequest = new OnboardingTemplateCreateRequest();
                createRequest.setName(
                        StringUtils.hasText(templateName)
                                ? templateName.trim()
                                : "Task Library - " + normalizedDepartmentTypeCode);
                createRequest.setDescription(description);
                createRequest.setStatus("ACTIVE");
                createRequest.setTemplateKind("TASK_LIBRARY");
                createRequest.setDepartmentTypeCode(normalizedDepartmentTypeCode);
                createRequest.setChecklists(parsed.getChecklists());
                upsertResponse = onboardingTemplateFacade.createOnboardingTemplate(createRequest);
                created = true;
            }
        } finally {
            BizContextHolder.clear();
        }

        TaskLibraryImportResponse response = new TaskLibraryImportResponse();
        response.setTemplateId(upsertResponse.getTemplateId());
        response.setDepartmentTypeCode(normalizedDepartmentTypeCode);
        response.setCreated(created);
        response.setImportedTasks(parsed.getImportedTasks());
        response.setTotalRows(parsed.getTotalRows());
        return BaseResponse.success(requestId, response);
    }

    private void assertDepartmentTypeActive(String companyId, String departmentTypeCode) {
        boolean exists = departmentTypeMapper
                .selectByCompanyAndStatus(companyId, "ACTIVE")
                .stream()
                .anyMatch(item -> departmentTypeCode.equalsIgnoreCase(item.getCode()));
        if (!exists) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "invalid or inactive departmentTypeCode");
        }
    }

    private static List<ChecklistTemplateUpdateItem> toChecklistUpdates(List<ChecklistTemplateCreateItem> checklists) {
        if (checklists == null || checklists.isEmpty()) {
            return List.of();
        }
        return checklists.stream().map(checklist -> {
            ChecklistTemplateUpdateItem updateItem = new ChecklistTemplateUpdateItem();
            updateItem.setName(checklist.getName());
            updateItem.setStage(checklist.getStage());
            updateItem.setSortOrder(checklist.getSortOrder());
            updateItem.setStatus(checklist.getStatus());
            updateItem.setTasks(toTaskUpdates(checklist.getTasks()));
            return updateItem;
        }).toList();
    }

    private static List<TaskTemplateUpdateItem> toTaskUpdates(List<TaskTemplateCreateItem> tasks) {
        if (tasks == null || tasks.isEmpty()) {
            return List.of();
        }
        return tasks.stream().map(task -> {
            TaskTemplateUpdateItem updateItem = new TaskTemplateUpdateItem();
            updateItem.setTitle(task.getTitle());
            updateItem.setDescription(task.getDescription());
            updateItem.setOwnerType(task.getOwnerType());
            updateItem.setOwnerRefId(task.getOwnerRefId());
            updateItem.setResponsibleDepartmentId(task.getResponsibleDepartmentId());
            updateItem.setDueDaysOffset(task.getDueDaysOffset());
            updateItem.setRequireAck(task.getRequireAck());
            updateItem.setRequireDoc(task.getRequireDoc());
            updateItem.setRequiresManagerApproval(task.getRequiresManagerApproval());
            updateItem.setApproverUserId(task.getApproverUserId());
            updateItem.setRequiredDocumentIds(task.getRequiredDocumentIds());
            updateItem.setSortOrder(task.getSortOrder());
            updateItem.setStatus(task.getStatus());
            return updateItem;
        }).toList();
    }

    private static String normalizeStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return null;
        }
        String normalized = status.trim().toUpperCase(Locale.US);
        if (!"ACTIVE".equals(normalized) && !"INACTIVE".equals(normalized) && !"DRAFT".equals(normalized)) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "invalid status value");
        }
        return normalized;
    }
}
