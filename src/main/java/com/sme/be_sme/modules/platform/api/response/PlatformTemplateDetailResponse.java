package com.sme.be_sme.modules.platform.api.response;

import lombok.Data;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
public class PlatformTemplateDetailResponse {
    private String templateId;
    private String name;
    private String description;
    private String status;
    private String templateKind;
    private String departmentTypeCode;
    private String level;
    private String createdBy;
    private Date createdAt;
    private Date updatedAt;

    private Integer checklistCount;
    private Integer taskCount;
    private Integer usedByCompanyCount;

    private List<PlatformTemplateDetailChecklistResponse> checklists = new ArrayList<>();
}