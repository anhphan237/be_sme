package com.sme.be_sme.modules.content.facade.impl;

import com.sme.be_sme.modules.content.api.request.DocumentAcknowledgeRequest;
import com.sme.be_sme.modules.content.api.request.DocumentListRequest;
import com.sme.be_sme.modules.content.api.request.DocumentUploadRequest;
import com.sme.be_sme.modules.content.api.response.DocumentAcknowledgeResponse;
import com.sme.be_sme.modules.content.api.response.DocumentListResponse;
import com.sme.be_sme.modules.content.api.response.DocumentUploadResponse;
import com.sme.be_sme.modules.content.facade.ContentFacade;
import com.sme.be_sme.modules.content.processor.DocumentAcknowledgeProcessor;
import com.sme.be_sme.modules.content.processor.DocumentListProcessor;
import com.sme.be_sme.modules.content.processor.DocumentUploadProcessor;
import com.sme.be_sme.shared.gateway.core.BaseOperationFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ContentFacadeImpl extends BaseOperationFacade implements ContentFacade {

    private final DocumentUploadProcessor documentUploadProcessor;
    private final DocumentListProcessor documentListProcessor;
    private final DocumentAcknowledgeProcessor documentAcknowledgeProcessor;

    @Override
    public DocumentUploadResponse uploadDocument(DocumentUploadRequest request) {
        return call(documentUploadProcessor, request, DocumentUploadResponse.class);
    }

    @Override
    public DocumentListResponse listDocuments(DocumentListRequest request) {
        return call(documentListProcessor, request, DocumentListResponse.class);
    }

    @Override
    public DocumentAcknowledgeResponse acknowledgeDocument(DocumentAcknowledgeRequest request) {
        return call(documentAcknowledgeProcessor, request, DocumentAcknowledgeResponse.class);
    }
}
