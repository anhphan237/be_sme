package com.sme.be_sme.modules.content.api;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sme.be_sme.modules.content.api.request.DocumentUploadRequest;
import com.sme.be_sme.modules.content.api.response.DocumentUploadResponse;
import com.sme.be_sme.modules.content.facade.ContentFacade;
import com.sme.be_sme.modules.content.service.CloudinaryUploadService;
import com.sme.be_sme.shared.api.BaseResponse;
import com.sme.be_sme.shared.constant.ErrorCodes;
import com.sme.be_sme.shared.exception.AppException;
import com.sme.be_sme.shared.gateway.core.BizContext;
import com.sme.be_sme.shared.gateway.core.BizContextHolder;
import com.sme.be_sme.shared.security.GatewayAuthGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/documents")
@RequiredArgsConstructor
public class DocumentUploadController {

    private static final String OPERATION_TYPE = "com.sme.content.document.upload";

    private final CloudinaryUploadService cloudinaryUploadService;
    private final ContentFacade contentFacade;
    private final GatewayAuthGuard authGuard;

    @PostMapping(value = "/upload", consumes = "multipart/form-data")
    public BaseResponse<DocumentUploadResponse> uploadFromFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "documentCategoryId", required = false) String documentCategoryId,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        if (file == null || file.isEmpty()) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "file is required");
        }
        String documentName = StringUtils.hasText(name) ? name.trim() : file.getOriginalFilename();
        if (!StringUtils.hasText(documentName)) {
            documentName = "Document";
        }

        String requestId = UUID.randomUUID().toString();
        BizContext ctx = authGuard.buildContext(
                OPERATION_TYPE,
                requestId,
                new ObjectNode(JsonNodeFactory.instance),
                authorization);

        String fileUrl;
        try {
            fileUrl = cloudinaryUploadService.upload(file);
        } catch (IOException e) {
            throw AppException.of(ErrorCodes.INTERNAL_ERROR, "failed to upload file: " + e.getMessage());
        }
        if (fileUrl == null) {
            throw AppException.of(ErrorCodes.INTERNAL_ERROR, "failed to upload file to Cloudinary");
        }

        DocumentUploadRequest request = new DocumentUploadRequest();
        request.setName(documentName);
        request.setFileUrl(fileUrl);
        request.setDescription(description);
        request.setDocumentCategoryId(documentCategoryId);

        BizContextHolder.set(ctx);
        try {
            DocumentUploadResponse response = contentFacade.uploadDocument(request);
            return BaseResponse.success(requestId, response);
        } finally {
            BizContextHolder.clear();
        }
    }
}
