package com.sme.be_sme.modules.content.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.billing.service.CompanyPlanQuotaService;
import com.sme.be_sme.modules.content.api.request.DocumentAttachmentAddRequest;
import com.sme.be_sme.modules.content.api.response.DocumentAttachmentAddResponse;
import com.sme.be_sme.modules.content.doceditor.DocumentAccessEvaluator;
import com.sme.be_sme.modules.content.doceditor.DocumentEditorConstants;
import com.sme.be_sme.modules.document.infrastructure.mapper.DocumentActivityLogMapper;
import com.sme.be_sme.modules.document.infrastructure.mapper.DocumentAttachmentMapper;
import com.sme.be_sme.modules.document.infrastructure.mapper.DocumentMapper;
import com.sme.be_sme.modules.document.infrastructure.persistence.entity.DocumentActivityLogEntity;
import com.sme.be_sme.modules.document.infrastructure.persistence.entity.DocumentAttachmentEntity;
import com.sme.be_sme.modules.document.infrastructure.persistence.entity.DocumentEntity;
import com.sme.be_sme.shared.constant.ErrorCodes;
import com.sme.be_sme.shared.exception.AppException;
import com.sme.be_sme.shared.gateway.core.BaseBizProcessor;
import com.sme.be_sme.shared.gateway.core.BizContext;
import com.sme.be_sme.shared.util.UuidGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.Locale;

@Component
@RequiredArgsConstructor
public class DocumentAttachmentAddProcessor extends BaseBizProcessor<BizContext> {

    private final ObjectMapper objectMapper;
    private final DocumentMapper documentMapper;
    private final DocumentAttachmentMapper documentAttachmentMapper;
    private final DocumentActivityLogMapper documentActivityLogMapper;
    private final DocumentAccessEvaluator documentAccessEvaluator;
    private final CompanyPlanQuotaService companyPlanQuotaService;

    @Override
    protected Object doProcess(BizContext context, JsonNode payload) {
        DocumentAttachmentAddRequest request = objectMapper.convertValue(payload, DocumentAttachmentAddRequest.class);
        if (context == null || !StringUtils.hasText(context.getTenantId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "tenantId is required");
        }
        if (request == null || !StringUtils.hasText(request.getDocumentId()) || !StringUtils.hasText(request.getFileUrl())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "documentId and fileUrl are required");
        }

        String url = request.getFileUrl().trim();
        String lower = url.toLowerCase(Locale.ROOT);
        if (!lower.startsWith("https://") && !lower.startsWith("http://")) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "fileUrl must be http(s)");
        }

        String companyId = context.getTenantId();
        companyPlanQuotaService.assertCanAddStorage(companyId, request.getFileSizeBytes());
        String operatorId = context.getOperatorId();
        String documentId = request.getDocumentId().trim();

        DocumentEntity doc = documentMapper.selectByPrimaryKey(documentId);
        if (doc == null || !companyId.equals(doc.getCompanyId())) {
            throw AppException.of(ErrorCodes.NOT_FOUND, "document not found");
        }
        documentAccessEvaluator.assertCanAccess(context, doc);

        String mediaKind = DocumentEditorConstants.MEDIA_KIND_FILE;
        if (StringUtils.hasText(request.getMediaKind())) {
            String mk = request.getMediaKind().trim().toUpperCase(Locale.ROOT);
            if (DocumentEditorConstants.MEDIA_KIND_VIDEO.equals(mk)) {
                mediaKind = DocumentEditorConstants.MEDIA_KIND_VIDEO;
            } else if (!DocumentEditorConstants.MEDIA_KIND_FILE.equals(mk)) {
                throw AppException.of(ErrorCodes.BAD_REQUEST, "mediaKind must be FILE or VIDEO");
            }
        }

        Date now = new Date();
        String id = UuidGenerator.generate();
        DocumentAttachmentEntity row = new DocumentAttachmentEntity();
        row.setDocumentAttachmentId(id);
        row.setCompanyId(companyId);
        row.setDocumentId(documentId);
        row.setFileUrl(url);
        row.setFileName(StringUtils.hasText(request.getFileName()) ? request.getFileName().trim() : null);
        row.setFileType(StringUtils.hasText(request.getFileType()) ? request.getFileType().trim() : null);
        row.setFileSizeBytes(request.getFileSizeBytes());
        row.setMediaKind(mediaKind);
        row.setStatus(DocumentEditorConstants.STATUS_ACTIVE);
        row.setUploadedBy(operatorId);
        row.setUploadedAt(now);
        if (documentAttachmentMapper.insert(row) != 1) {
            throw AppException.of(ErrorCodes.INTERNAL_ERROR, "failed to add attachment");
        }

        DocumentActivityLogEntity log = new DocumentActivityLogEntity();
        log.setDocumentActivityLogId(UuidGenerator.generate());
        log.setCompanyId(companyId);
        log.setDocumentId(documentId);
        log.setAction(DocumentEditorConstants.ACTION_ATTACHMENT_ADD);
        log.setActorUserId(operatorId);
        log.setDetailJson("{\"documentAttachmentId\":\"" + id + "\",\"mediaKind\":\"" + mediaKind + "\"}");
        log.setCreatedAt(now);
        documentActivityLogMapper.insert(log);

        DocumentAttachmentAddResponse response = new DocumentAttachmentAddResponse();
        response.setDocumentAttachmentId(id);
        response.setDocumentId(documentId);
        response.setMediaKind(mediaKind);
        return response;
    }
}
