package com.sme.be_sme.modules.content.api.response;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
public class DocumentAccessRuleAddResponse {
    private String documentAccessRuleId;
    private String documentId;
    private Date createdAt;
}
