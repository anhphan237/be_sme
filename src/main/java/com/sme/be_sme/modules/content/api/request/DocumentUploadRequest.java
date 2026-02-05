package com.sme.be_sme.modules.content.api.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DocumentUploadRequest {
    /** Display name of the document */
    private String name;
    /** URL or S3 path to the file */
    private String fileUrl;
    /** Optional description */
    private String description;
    /** Optional category id */
    private String documentCategoryId;
}
