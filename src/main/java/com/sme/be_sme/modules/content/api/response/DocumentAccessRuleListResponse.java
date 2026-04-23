package com.sme.be_sme.modules.content.api.response;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.List;

@Getter
@Setter
public class DocumentAccessRuleListResponse {
    private String documentId;
    private List<Row> rules;

    @Getter
    @Setter
    public static class Row {
        private String documentAccessRuleId;
        private String roleId;
        private String departmentId;
        private String status;
        private Date createdAt;
    }
}
