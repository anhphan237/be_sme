package com.sme.be_sme.modules.onboarding.api.request;

import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
public class TaskTemplateCreateItem {
    private String title;
    private String description;
    private String ownerType;
    private String ownerRefId;
    /**
     * Preferred alias for department ownership.
     * Used when {@code ownerType=DEPARTMENT}; maps to {@code ownerRefId}.
     */
    private String responsibleDepartmentId;
    private Integer dueDaysOffset;
    private Boolean requireAck;
    private Boolean requireDoc;
    private Boolean requiresManagerApproval;
    /** When set, only this user ({@code users.user_id}) may approve/reject (overrides line manager). */
    private String approverUserId;
    /** Required when {@code requireAck=true}: document ids selected from content library. */
    private List<String> requiredDocumentIds;
    private Integer sortOrder;
    private String status;
}
