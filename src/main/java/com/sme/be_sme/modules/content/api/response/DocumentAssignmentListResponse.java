package com.sme.be_sme.modules.content.api.response;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.List;

@Getter
@Setter
public class DocumentAssignmentListResponse {
    private String documentId;
    private List<AssignmentRow> items;

    @Getter
    @Setter
    public static class AssignmentRow {
        private String documentAssignmentId;
        private String assigneeUserId;
        private String assignedByUserId;
        private String status;
        private Date assignedAt;
    }
}
