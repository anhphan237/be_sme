package com.sme.be_sme.modules.onboarding.api.request;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class OnboardingTemplateCreateRequest {
    private String name;
    private String description;
    private String status;
    private String createdBy;
    private List<ChecklistTemplateCreateItem> checklists;
}
