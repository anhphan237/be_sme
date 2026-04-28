package com.sme.be_sme.modules.platform.api.response;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class PlatformTemplateDetailTaskResponse {
    private String checklistTemplateId;
    private String taskTemplateId;
    private String title;
    private String description;
    private String ownerType;
    private String ownerRefId;
    private Integer dueDaysOffset;

    private Boolean requireAck;
    private Boolean requireDoc;
    private Boolean requiresManagerApproval;
    private String approverUserId;

    private Integer sortOrder;
    private String status;

    private List<String> requiredDocumentIds = new ArrayList<>();
}