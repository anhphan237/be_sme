package com.sme.be_sme.modules.content.api.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DocumentAccessRuleAddRequest {
    private String documentId;
    /** Optional when departmentId is set */
    private String roleId;
    /** Optional when roleId is set */
    private String departmentId;
}
