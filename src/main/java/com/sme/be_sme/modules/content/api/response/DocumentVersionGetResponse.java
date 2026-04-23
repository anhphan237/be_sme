package com.sme.be_sme.modules.content.api.response;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
public class DocumentVersionGetResponse {
    private String documentVersionId;
    private String documentId;
    private Integer versionNo;
    private String fileUrl;
    private JsonNode contentJson;
    private Date uploadedAt;
    private String uploadedBy;
}
