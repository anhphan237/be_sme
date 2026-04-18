package com.sme.be_sme.modules.onboarding.api.request;

import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChecklistTemplateUpdateItem {
    private String checklistTemplateId;
    private String name;
    private String stage;
    private Integer sortOrder;
    private String status;
    private List<TaskTemplateUpdateItem> tasks;
}
