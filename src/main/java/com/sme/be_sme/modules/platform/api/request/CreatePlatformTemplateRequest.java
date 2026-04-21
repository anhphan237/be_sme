package com.sme.be_sme.modules.platform.api.request;

import com.sme.be_sme.modules.onboarding.api.request.ChecklistTemplateCreateItem;
import lombok.Data;

import java.util.List;

@Data
public class CreatePlatformTemplateRequest {
    private String name;
    private String description;
    private String status;
    private String createdBy;
    private String templateKind;
    private String departmentTypeCode;
    private List<ChecklistTemplateCreateItem> checklists;
}
