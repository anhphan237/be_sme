package com.sme.be_sme.modules.content.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.content.api.request.DocumentEditorListRequest;
import com.sme.be_sme.modules.content.api.response.DocumentEditorListResponse;
import com.sme.be_sme.modules.content.doceditor.DocumentAccessEvaluator;
import com.sme.be_sme.modules.content.doceditor.DocumentEditorConstants;
import com.sme.be_sme.modules.document.infrastructure.mapper.DocumentMapper;
import com.sme.be_sme.modules.document.infrastructure.persistence.entity.DocumentEntity;
import com.sme.be_sme.shared.constant.ErrorCodes;
import com.sme.be_sme.shared.exception.AppException;
import com.sme.be_sme.shared.gateway.core.BaseBizProcessor;
import com.sme.be_sme.shared.gateway.core.BizContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DocumentEditorListProcessor extends BaseBizProcessor<BizContext> {

    private static final int DEFAULT_PAGE_SIZE = 50;
    private static final int MAX_PAGE_SIZE = 100;

    private final ObjectMapper objectMapper;
    private final DocumentMapper documentMapper;
    private final DocumentAccessEvaluator documentAccessEvaluator;

    @Override
    protected Object doProcess(BizContext context, JsonNode payload) {
        DocumentEditorListRequest request = payload == null || payload.isNull()
                ? new DocumentEditorListRequest()
                : objectMapper.convertValue(payload, DocumentEditorListRequest.class);
        if (context == null || !StringUtils.hasText(context.getTenantId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "tenantId is required");
        }

        String companyId = context.getTenantId();
        int page = request.getPage() != null && request.getPage() > 0 ? request.getPage() : 1;
        int pageSize = request.getPageSize() != null && request.getPageSize() > 0
                ? Math.min(request.getPageSize(), MAX_PAGE_SIZE)
                : DEFAULT_PAGE_SIZE;
        String titleQuery = StringUtils.hasText(request.getTitleQuery()) ? request.getTitleQuery().trim() : null;
        int offset = (page - 1) * pageSize;

        String contentKind = DocumentEditorConstants.CONTENT_KIND_EDITOR;
        long total;
        List<DocumentEntity> docs;
        if (documentAccessEvaluator.isManagementBypass(context.getRoles())) {
            total = documentMapper.countEditorDocumentsHrPaged(companyId, contentKind, titleQuery);
            docs = documentMapper.selectEditorDocumentsHrPaged(companyId, contentKind, titleQuery, pageSize, offset);
        } else {
            DocumentAccessEvaluator.EditorAccessSubject subject = documentAccessEvaluator.resolveSubject(context);
            total = documentMapper.countEditorDocumentsAccessiblePaged(
                    companyId, contentKind, titleQuery, subject.roleIds(), subject.departmentId());
            docs = documentMapper.selectEditorDocumentsAccessiblePaged(
                    companyId, contentKind, titleQuery, subject.roleIds(), subject.departmentId(), pageSize, offset);
        }
        if (docs == null) {
            docs = new ArrayList<>();
        }

        List<DocumentEditorListResponse.Item> items = new ArrayList<>();
        for (DocumentEntity d : docs) {
            DocumentEditorListResponse.Item item = new DocumentEditorListResponse.Item();
            item.setDocumentId(d.getDocumentId());
            item.setTitle(d.getTitle());
            item.setStatus(d.getStatus());
            item.setContentKind(d.getContentKind());
            item.setUpdatedAt(d.getUpdatedAt());
            item.setPublished(d.getPublishedAt() != null);
            items.add(item);
        }

        DocumentEditorListResponse response = new DocumentEditorListResponse();
        response.setItems(items);
        response.setTotalCount(total);
        response.setPage(page);
        response.setPageSize(pageSize);
        return response;
    }
}
