package com.sme.be_sme.modules.content.facade;

import com.sme.be_sme.modules.content.api.request.*;
import com.sme.be_sme.modules.content.api.response.*;
import com.sme.be_sme.shared.gateway.annotation.OperationType;
import com.sme.be_sme.shared.gateway.core.OperationFacadeProvider;

public interface DocumentEditorFacade extends OperationFacadeProvider {

    @OperationType("com.sme.document.createDraft")
    DocumentCreateDraftResponse createDraft(DocumentCreateDraftRequest request);

    @OperationType("com.sme.document.updateDraft")
    DocumentEditorSaveResponse updateDraft(DocumentUpdateDraftRequest request);

    @OperationType("com.sme.document.autosave")
    DocumentEditorSaveResponse autosave(DocumentUpdateDraftRequest request);

    @OperationType("com.sme.document.publish")
    DocumentPublishResponse publish(DocumentPublishRequest request);

    @OperationType("com.sme.document.detail")
    DocumentEditorDetailResponse detail(DocumentEditorDetailRequest request);

    @OperationType("com.sme.document.list")
    DocumentEditorListResponse listEditorDocuments(DocumentEditorListRequest request);

    @OperationType("com.sme.document.version.list")
    DocumentVersionListResponse listVersions(DocumentVersionListRequest request);

    @OperationType("com.sme.document.version.get")
    DocumentVersionGetResponse getVersion(DocumentVersionGetRequest request);

    @OperationType("com.sme.document.version.compare")
    DocumentVersionCompareResponse compareVersions(DocumentVersionCompareRequest request);

    @OperationType("com.sme.document.read.mark")
    DocumentReadMarkResponse markRead(DocumentReadMarkRequest request);

    @OperationType("com.sme.document.read.list")
    DocumentReadListResponse listReads(DocumentReadListRequest request);

    @OperationType("com.sme.document.folder.create")
    DocumentFolderCreateResponse createFolder(DocumentFolderCreateRequest request);

    @OperationType("com.sme.document.folder.rename")
    DocumentFolderRenameResponse renameFolder(DocumentFolderRenameRequest request);

    @OperationType("com.sme.document.folder.move")
    DocumentFolderMoveResponse moveFolder(DocumentFolderMoveRequest request);

    @OperationType("com.sme.document.folder.list")
    DocumentFolderListResponse listFolders(DocumentFolderListRequest request);

    @OperationType("com.sme.document.folder.tree")
    DocumentFolderTreeResponse folderTree(DocumentFolderTreeRequest request);

    @OperationType("com.sme.document.folder.treeWithDocuments")
    DocumentFolderTreeWithDocumentsResponse folderTreeWithDocuments(DocumentFolderTreeWithDocumentsRequest request);

    @OperationType("com.sme.document.folder.delete")
    DocumentFolderDeleteResponse deleteFolder(DocumentFolderDeleteRequest request);

    @OperationType("com.sme.document.folder.addDocument")
    DocumentFolderAddDocumentResponse addDocumentToFolder(DocumentFolderAddDocumentRequest request);

    @OperationType("com.sme.document.folder.removeDocument")
    DocumentFolderRemoveDocumentResponse removeDocumentFromFolder(DocumentFolderRemoveDocumentRequest request);

    @OperationType("com.sme.document.comment.add")
    DocumentCommentAddResponse addComment(DocumentCommentAddRequest request);

    @OperationType("com.sme.document.comment.list")
    DocumentCommentListResponse listComments(DocumentCommentListRequest request);

    @OperationType("com.sme.document.comment.tree")
    DocumentCommentTreeResponse treeComments(DocumentCommentTreeRequest request);

    @OperationType("com.sme.document.comment.delete")
    DocumentCommentDeleteResponse deleteComment(DocumentCommentDeleteRequest request);

    @OperationType("com.sme.document.comment.update")
    DocumentCommentUpdateResponse updateComment(DocumentCommentUpdateRequest request);

    @OperationType("com.sme.document.accessRule.add")
    DocumentAccessRuleAddResponse addAccessRule(DocumentAccessRuleAddRequest request);

    @OperationType("com.sme.document.accessRule.remove")
    DocumentAccessRuleRemoveResponse removeAccessRule(DocumentAccessRuleRemoveRequest request);

    @OperationType("com.sme.document.accessRule.list")
    DocumentAccessRuleListResponse listAccessRules(DocumentAccessRuleListRequest request);

    @OperationType("com.sme.document.link.add")
    DocumentLinkAddResponse addLink(DocumentLinkAddRequest request);

    @OperationType("com.sme.document.link.remove")
    DocumentLinkRemoveResponse removeLink(DocumentLinkRemoveRequest request);

    @OperationType("com.sme.document.link.list")
    DocumentLinkListResponse listLinks(DocumentLinkListRequest request);

    @OperationType("com.sme.document.assignment.assign")
    DocumentAssignmentAssignResponse assignDocument(DocumentAssignmentAssignRequest request);

    @OperationType("com.sme.document.assignment.unassign")
    DocumentAssignmentUnassignResponse unassignDocument(DocumentAssignmentUnassignRequest request);

    @OperationType("com.sme.document.assignment.list")
    DocumentAssignmentListResponse listAssignments(DocumentAssignmentListRequest request);

    @OperationType("com.sme.document.attachment.add")
    DocumentAttachmentAddResponse addAttachment(DocumentAttachmentAddRequest request);

    @OperationType("com.sme.document.attachment.remove")
    DocumentAttachmentRemoveResponse removeAttachment(DocumentAttachmentRemoveRequest request);

    @OperationType("com.sme.document.attachment.list")
    DocumentAttachmentListResponse listAttachments(DocumentAttachmentListRequest request);

    @OperationType("com.sme.document.block.list")
    DocumentBlockListResponse listBlocks(DocumentBlockListRequest request);

    @OperationType("com.sme.document.block.create")
    DocumentBlockMutateResponse createBlock(DocumentBlockCreateRequest request);

    @OperationType("com.sme.document.block.update")
    DocumentBlockMutateResponse updateBlock(DocumentBlockUpdateRequest request);

    @OperationType("com.sme.document.block.move")
    DocumentBlockMutateResponse moveBlock(DocumentBlockMoveRequest request);

    @OperationType("com.sme.document.block.delete")
    DocumentBlockMutateResponse deleteBlock(DocumentBlockDeleteRequest request);
}
