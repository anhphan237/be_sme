package com.sme.be_sme.modules.onboarding.api.request;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ChecklistTemplateCreateItem {
    private String name;
    private String stage;
    private Integer sortOrder;
    private String status;
    private List<TaskTemplateCreateItem> tasks;
}
