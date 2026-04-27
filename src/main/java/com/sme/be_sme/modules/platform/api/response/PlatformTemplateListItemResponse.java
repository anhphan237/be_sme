package com.sme.be_sme.modules.platform.api.response;

import lombok.Data;

import java.util.Date;

@Data
public class PlatformTemplateListItemResponse {
    private String templateId;
    private String name;
    private String description;
    private String status;
    private String templateKind;
    private String departmentTypeCode;
    private String level;

    private Integer checklistCount;
    private Integer taskCount;
    private Integer usedByCompanyCount;

    private Date createdAt;
    private Date updatedAt;
}