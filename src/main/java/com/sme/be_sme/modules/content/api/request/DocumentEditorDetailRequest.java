package com.sme.be_sme.modules.content.api.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DocumentEditorDetailRequest {
    private String documentId;
    /** Max activity rows; default 50 */
    private Integer activityLimit;
    /** Max read receipt rows; default 100 */
    private Integer readLimit;
    /** Max comment rows; default 200 */
    private Integer commentLimit;
    /**
     * Comma-separated: links,assignments,attachments. Blank = include all three (Phase 3 default).
     */
    private String include;
    /** Max rows per Phase-3 section (links out, links in, assignments, attachments); default 100 */
    private Integer relationLimit;
}
