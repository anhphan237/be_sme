package com.sme.be_sme.modules.content.facade;

import com.sme.be_sme.modules.content.api.request.DocumentAcknowledgeRequest;
import com.sme.be_sme.modules.content.api.request.DocumentListRequest;
import com.sme.be_sme.modules.content.api.request.DocumentUploadRequest;
import com.sme.be_sme.modules.content.api.response.DocumentAcknowledgeResponse;
import com.sme.be_sme.modules.content.api.response.DocumentListResponse;
import com.sme.be_sme.modules.content.api.response.DocumentUploadResponse;
import com.sme.be_sme.shared.gateway.annotation.OperationType;
import com.sme.be_sme.shared.gateway.core.OperationFacadeProvider;

public interface ContentFacade extends OperationFacadeProvider {

    @OperationType("com.sme.content.document.upload")
    DocumentUploadResponse uploadDocument(DocumentUploadRequest request);

    @OperationType("com.sme.content.document.list")
    DocumentListResponse listDocuments(DocumentListRequest request);

    @OperationType("com.sme.content.document.acknowledge")
    DocumentAcknowledgeResponse acknowledgeDocument(DocumentAcknowledgeRequest request);
}
