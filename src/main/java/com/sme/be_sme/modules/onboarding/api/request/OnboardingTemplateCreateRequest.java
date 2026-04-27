package com.sme.be_sme.modules.onboarding.api.request;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class OnboardingTemplateCreateRequest {
    /** Optional: clone from an existing template (usually PLATFORM level). */
    private String sourceTemplateId;
    /** Optional: source template level when cloning. */
    private String sourceTemplateLevel;
    private String name;
    private String description;
    private String status;
    private String createdBy;
    private String templateKind;
    private String departmentTypeCode;
    private List<ChecklistTemplateCreateItem> checklists;
}
