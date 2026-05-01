package com.sme.be_sme.modules.content.facade.impl;

import com.sme.be_sme.modules.content.api.request.*;
import com.sme.be_sme.modules.content.api.response.*;
import com.sme.be_sme.modules.content.facade.DocumentEditorFacade;
import com.sme.be_sme.modules.content.processor.*;
import com.sme.be_sme.shared.gateway.core.BaseOperationFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DocumentEditorFacadeImpl extends BaseOperationFacade implements DocumentEditorFacade {

    private final DocumentCreateDraftProcessor documentCreateDraftProcessor;
    private final DocumentUpdateDraftProcessor documentUpdateDraftProcessor;
    private final DocumentPublishProcessor documentPublishProcessor;
    private final DocumentSoftDeleteProcessor documentSoftDeleteProcessor;
    private final DocumentEditorDetailProcessor documentEditorDetailProcessor;
    private final DocumentEditorListProcessor documentEditorListProcessor;
    private final DocumentVersionListProcessor documentVersionListProcessor;
    private final DocumentVersionGetProcessor documentVersionGetProcessor;
    private final DocumentVersionCompareProcessor documentVersionCompareProcessor;
    private final DocumentReadMarkProcessor documentReadMarkProcessor;
    private final DocumentReadListProcessor documentReadListProcessor;
    private final DocumentFolderCreateProcessor documentFolderCreateProcessor;
    private final DocumentFolderRenameProcessor documentFolderRenameProcessor;
    private final DocumentFolderMoveProcessor documentFolderMoveProcessor;
    private final DocumentFolderListProcessor documentFolderListProcessor;
    private final DocumentFolderTreeProcessor documentFolderTreeProcessor;
    private final DocumentFolderTreeWithDocumentsProcessor documentFolderTreeWithDocumentsProcessor;
    private final DocumentFolderDeleteProcessor documentFolderDeleteProcessor;
    private final DocumentFolderAddDocumentProcessor documentFolderAddDocumentProcessor;
    private final DocumentFolderRemoveDocumentProcessor documentFolderRemoveDocumentProcessor;
    private final DocumentCommentAddProcessor documentCommentAddProcessor;
    private final DocumentCommentListProcessor documentCommentListProcessor;
    private final DocumentCommentTreeProcessor documentCommentTreeProcessor;
    private final DocumentCommentDeleteProcessor documentCommentDeleteProcessor;
    private final DocumentCommentUpdateProcessor documentCommentUpdateProcessor;
    private final DocumentAccessRuleAddProcessor documentAccessRuleAddProcessor;
    private final DocumentAccessRuleRemoveProcessor documentAccessRuleRemoveProcessor;
    private final DocumentAccessRuleListProcessor documentAccessRuleListProcessor;
    private final DocumentLinkAddProcessor documentLinkAddProcessor;
    private final DocumentLinkRemoveProcessor documentLinkRemoveProcessor;
    private final DocumentLinkListProcessor documentLinkListProcessor;
    private final DocumentAssignmentAssignProcessor documentAssignmentAssignProcessor;
    private final DocumentAssignmentUnassignProcessor documentAssignmentUnassignProcessor;
    private final DocumentAssignmentListProcessor documentAssignmentListProcessor;
    private final DocumentAttachmentAddProcessor documentAttachmentAddProcessor;
    private final DocumentAttachmentRemoveProcessor documentAttachmentRemoveProcessor;
    private final DocumentAttachmentListProcessor documentAttachmentListProcessor;
    private final DocumentBlockListProcessor documentBlockListProcessor;
    private final DocumentBlockCreateProcessor documentBlockCreateProcessor;
    private final DocumentBlockUpdateProcessor documentBlockUpdateProcessor;
    private final DocumentBlockMoveProcessor documentBlockMoveProcessor;
    private final DocumentBlockDeleteProcessor documentBlockDeleteProcessor;

    @Override
    public DocumentCreateDraftResponse createDraft(DocumentCreateDraftRequest request) {
        return call(documentCreateDraftProcessor, request, DocumentCreateDraftResponse.class);
    }

    @Override
    public DocumentEditorSaveResponse updateDraft(DocumentUpdateDraftRequest request) {
        return call(documentUpdateDraftProcessor, request, DocumentEditorSaveResponse.class);
    }

    @Override
    public DocumentEditorSaveResponse autosave(DocumentUpdateDraftRequest request) {
        return call(documentUpdateDraftProcessor, request, DocumentEditorSaveResponse.class);
    }

    @Override
    public DocumentPublishResponse publish(DocumentPublishRequest request) {
        return call(documentPublishProcessor, request, DocumentPublishResponse.class);
    }

    @Override
    public DocumentSoftDeleteResponse softDelete(DocumentSoftDeleteRequest request) {
        return call(documentSoftDeleteProcessor, request, DocumentSoftDeleteResponse.class);
    }

    @Override
    public DocumentEditorDetailResponse detail(DocumentEditorDetailRequest request) {
        return call(documentEditorDetailProcessor, request, DocumentEditorDetailResponse.class);
    }

    @Override
    public DocumentEditorListResponse listEditorDocuments(DocumentEditorListRequest request) {
        return call(documentEditorListProcessor, request, DocumentEditorListResponse.class);
    }

    @Override
    public DocumentVersionListResponse listVersions(DocumentVersionListRequest request) {
        return call(documentVersionListProcessor, request, DocumentVersionListResponse.class);
    }

    @Override
    public DocumentVersionGetResponse getVersion(DocumentVersionGetRequest request) {
        return call(documentVersionGetProcessor, request, DocumentVersionGetResponse.class);
    }

    @Override
    public DocumentVersionCompareResponse compareVersions(DocumentVersionCompareRequest request) {
        return call(documentVersionCompareProcessor, request, DocumentVersionCompareResponse.class);
    }

    @Override
    public DocumentReadMarkResponse markRead(DocumentReadMarkRequest request) {
        return call(documentReadMarkProcessor, request, DocumentReadMarkResponse.class);
    }

    @Override
    public DocumentReadListResponse listReads(DocumentReadListRequest request) {
        return call(documentReadListProcessor, request, DocumentReadListResponse.class);
    }

    @Override
    public DocumentFolderCreateResponse createFolder(DocumentFolderCreateRequest request) {
        return call(documentFolderCreateProcessor, request, DocumentFolderCreateResponse.class);
    }

    @Override
    public DocumentFolderRenameResponse renameFolder(DocumentFolderRenameRequest request) {
        return call(documentFolderRenameProcessor, request, DocumentFolderRenameResponse.class);
    }

    @Override
    public DocumentFolderMoveResponse moveFolder(DocumentFolderMoveRequest request) {
        return call(documentFolderMoveProcessor, request, DocumentFolderMoveResponse.class);
    }

    @Override
    public DocumentFolderListResponse listFolders(DocumentFolderListRequest request) {
        return call(documentFolderListProcessor, request, DocumentFolderListResponse.class);
    }

    @Override
    public DocumentFolderTreeResponse folderTree(DocumentFolderTreeRequest request) {
        return call(documentFolderTreeProcessor, request, DocumentFolderTreeResponse.class);
    }

    @Override
    public DocumentFolderTreeWithDocumentsResponse folderTreeWithDocuments(DocumentFolderTreeWithDocumentsRequest request) {
        return call(documentFolderTreeWithDocumentsProcessor, request, DocumentFolderTreeWithDocumentsResponse.class);
    }

    @Override
    public DocumentFolderDeleteResponse deleteFolder(DocumentFolderDeleteRequest request) {
        return call(documentFolderDeleteProcessor, request, DocumentFolderDeleteResponse.class);
    }

    @Override
    public DocumentFolderAddDocumentResponse addDocumentToFolder(DocumentFolderAddDocumentRequest request) {
        return call(documentFolderAddDocumentProcessor, request, DocumentFolderAddDocumentResponse.class);
    }

    @Override
    public DocumentFolderRemoveDocumentResponse removeDocumentFromFolder(DocumentFolderRemoveDocumentRequest request) {
        return call(documentFolderRemoveDocumentProcessor, request, DocumentFolderRemoveDocumentResponse.class);
    }

    @Override
    public DocumentCommentAddResponse addComment(DocumentCommentAddRequest request) {
        return call(documentCommentAddProcessor, request, DocumentCommentAddResponse.class);
    }

    @Override
    public DocumentCommentListResponse listComments(DocumentCommentListRequest request) {
        return call(documentCommentListProcessor, request, DocumentCommentListResponse.class);
    }

    @Override
    public DocumentCommentTreeResponse treeComments(DocumentCommentTreeRequest request) {
        return call(documentCommentTreeProcessor, request, DocumentCommentTreeResponse.class);
    }

    @Override
    public DocumentCommentDeleteResponse deleteComment(DocumentCommentDeleteRequest request) {
        return call(documentCommentDeleteProcessor, request, DocumentCommentDeleteResponse.class);
    }

    @Override
    public DocumentCommentUpdateResponse updateComment(DocumentCommentUpdateRequest request) {
        return call(documentCommentUpdateProcessor, request, DocumentCommentUpdateResponse.class);
    }

    @Override
    public DocumentAccessRuleAddResponse addAccessRule(DocumentAccessRuleAddRequest request) {
        return call(documentAccessRuleAddProcessor, request, DocumentAccessRuleAddResponse.class);
    }

    @Override
    public DocumentAccessRuleRemoveResponse removeAccessRule(DocumentAccessRuleRemoveRequest request) {
        return call(documentAccessRuleRemoveProcessor, request, DocumentAccessRuleRemoveResponse.class);
    }

    @Override
    public DocumentAccessRuleListResponse listAccessRules(DocumentAccessRuleListRequest request) {
        return call(documentAccessRuleListProcessor, request, DocumentAccessRuleListResponse.class);
    }

    @Override
    public DocumentLinkAddResponse addLink(DocumentLinkAddRequest request) {
        return call(documentLinkAddProcessor, request, DocumentLinkAddResponse.class);
    }

    @Override
    public DocumentLinkRemoveResponse removeLink(DocumentLinkRemoveRequest request) {
        return call(documentLinkRemoveProcessor, request, DocumentLinkRemoveResponse.class);
    }

    @Override
    public DocumentLinkListResponse listLinks(DocumentLinkListRequest request) {
        return call(documentLinkListProcessor, request, DocumentLinkListResponse.class);
    }

    @Override
    public DocumentAssignmentAssignResponse assignDocument(DocumentAssignmentAssignRequest request) {
        return call(documentAssignmentAssignProcessor, request, DocumentAssignmentAssignResponse.class);
    }

    @Override
    public DocumentAssignmentUnassignResponse unassignDocument(DocumentAssignmentUnassignRequest request) {
        return call(documentAssignmentUnassignProcessor, request, DocumentAssignmentUnassignResponse.class);
    }

    @Override
    public DocumentAssignmentListResponse listAssignments(DocumentAssignmentListRequest request) {
        return call(documentAssignmentListProcessor, request, DocumentAssignmentListResponse.class);
    }

    @Override
    public DocumentAttachmentAddResponse addAttachment(DocumentAttachmentAddRequest request) {
        return call(documentAttachmentAddProcessor, request, DocumentAttachmentAddResponse.class);
    }

    @Override
    public DocumentAttachmentRemoveResponse removeAttachment(DocumentAttachmentRemoveRequest request) {
        return call(documentAttachmentRemoveProcessor, request, DocumentAttachmentRemoveResponse.class);
    }

    @Override
    public DocumentAttachmentListResponse listAttachments(DocumentAttachmentListRequest request) {
        return call(documentAttachmentListProcessor, request, DocumentAttachmentListResponse.class);
    }

    @Override
    public DocumentBlockListResponse listBlocks(DocumentBlockListRequest request) {
        return call(documentBlockListProcessor, request, DocumentBlockListResponse.class);
    }

    @Override
    public DocumentBlockMutateResponse createBlock(DocumentBlockCreateRequest request) {
        return call(documentBlockCreateProcessor, request, DocumentBlockMutateResponse.class);
    }

    @Override
    public DocumentBlockMutateResponse updateBlock(DocumentBlockUpdateRequest request) {
        return call(documentBlockUpdateProcessor, request, DocumentBlockMutateResponse.class);
    }

    @Override
    public DocumentBlockMutateResponse moveBlock(DocumentBlockMoveRequest request) {
        return call(documentBlockMoveProcessor, request, DocumentBlockMutateResponse.class);
    }

    @Override
    public DocumentBlockMutateResponse deleteBlock(DocumentBlockDeleteRequest request) {
        return call(documentBlockDeleteProcessor, request, DocumentBlockMutateResponse.class);
    }
}
