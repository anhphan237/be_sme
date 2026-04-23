package com.sme.be_sme.modules.content.api.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DocumentAssignmentAssignResponse {
    private String documentAssignmentId;
    private String documentId;
    private String assigneeUserId;
    private String status;
}
