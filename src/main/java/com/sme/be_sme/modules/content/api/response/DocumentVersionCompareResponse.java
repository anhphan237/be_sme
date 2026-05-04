package com.sme.be_sme.modules.content.api.response;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DocumentVersionCompareResponse {
    private String documentId;
    private boolean equal;
    private String fromDocumentVersionId;
    private String toDocumentVersionId;
    private Integer fromVersionNo;
    private Integer toVersionNo;
    /** Metadata only: top-level key deltas and limited changedPaths */
    private JsonNode summary;
    /**
     * Detailed diffs for FE rendering (GitHub-style hints), limited by
     * {@code DocumentEditorConstants.VERSION_COMPARE_MAX_PATHS}.
     * Shape: [{type,path,fromValue,toValue}]
     */
    private JsonNode changes;
    /**
     * Block-aware diffs (line-by-line equivalent where one line = one top-level block).
     * Shape: [{type,fromBlockIndex,toBlockIndex,blockType,path,fromPreview,toPreview,fromValue,toValue}]
     */
    private JsonNode blockChanges;
}
