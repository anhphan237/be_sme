package com.sme.be_sme.modules.identity.bulk.api;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sme.be_sme.modules.identity.bulk.BulkUserImportService;
import com.sme.be_sme.modules.identity.bulk.api.response.BulkUserImportCommitResponse;
import com.sme.be_sme.modules.identity.bulk.api.response.BulkUserImportValidateResponse;
import com.sme.be_sme.shared.api.BaseResponse;
import com.sme.be_sme.shared.gateway.core.BizContext;
import com.sme.be_sme.shared.security.GatewayAuthGuard;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/users/bulk-import")
@RequiredArgsConstructor
public class BulkUserImportController {

    private static final String OP_DOWNLOAD_TEMPLATE = "com.sme.identity.user.bulkImport.downloadTemplate";
    private static final String OP_VALIDATE = "com.sme.identity.user.bulkImport.validate";
    private static final String OP_COMMIT = "com.sme.identity.user.bulkImport.commit";

    private final GatewayAuthGuard authGuard;
    private final BulkUserImportService bulkUserImportService;

    @GetMapping("/excel-template")
    public ResponseEntity<byte[]> downloadTemplate(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        String requestId = UUID.randomUUID().toString();
        authGuard.buildContext(OP_DOWNLOAD_TEMPLATE, requestId, new ObjectNode(JsonNodeFactory.instance), authorization);

        byte[] bytes = bulkUserImportService.buildTemplateWorkbook();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(
                MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.setContentDisposition(
                ContentDisposition.attachment().filename("bulk-user-import-template.xlsx").build());
        headers.set("X-Request-Id", requestId);
        return ResponseEntity.ok().headers(headers).body(bytes);
    }

    @PostMapping(value = "/validate", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public BaseResponse<BulkUserImportValidateResponse> validate(
            @RequestParam("file") MultipartFile file,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        String requestId = UUID.randomUUID().toString();
        BizContext ctx = authGuard.buildContext(OP_VALIDATE, requestId, new ObjectNode(JsonNodeFactory.instance), authorization);

        BulkUserImportValidateResponse response = bulkUserImportService.validate(file, ctx.getTenantId());
        return BaseResponse.success(requestId, response);
    }

    @PostMapping(value = "/commit", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public BaseResponse<BulkUserImportCommitResponse> commit(
            @RequestParam("file") MultipartFile file,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        String requestId = UUID.randomUUID().toString();
        BizContext ctx = authGuard.buildContext(OP_COMMIT, requestId, new ObjectNode(JsonNodeFactory.instance), authorization);

        BulkUserImportCommitResponse response = bulkUserImportService.commit(file, ctx);
        return BaseResponse.success(requestId, response);
    }
}
