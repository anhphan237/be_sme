package com.sme.be_sme.modules.content.api.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DocumentListRequest {
    /** Optional: filter by category */
    private String documentCategoryId;

    /** When true, return only FILE documents uploaded by the current JWT user ({@code created_by}). */
    private Boolean onlyMine;

    /** 1-based page index; defaults to 1 */
    private Integer page;

    /** Page size; defaults to 50, capped at 100 */
    private Integer pageSize;
}
