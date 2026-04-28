package com.sme.be_sme.modules.platform.api.response;

import lombok.Data;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
public class PlatformTemplateDetailChecklistResponse {
    private String checklistTemplateId;
    private String name;
    private String stage;
    private Integer deadlineDays;
    private Integer sortOrder;
    private String status;
    private Date createdAt;
    private Date updatedAt;

    private List<PlatformTemplateDetailTaskResponse> tasks = new ArrayList<>();
}