package com.sme.be_sme.modules.onboarding.api.response;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class OnboardingTemplateGetResponse {
    private String templateId;
    private String name;
    private String status;
    private String description;

    private List<ChecklistTemplateItemResponse> checklists;
    private List<TaskTemplateItemResponse> baselineTasks;

    @Getter
    @Setter
    public static class ChecklistTemplateItemResponse {
        private String checklistTemplateId;
        private String name;
        private String description;
        private Integer orderNo;
        private String status;
    }

    @Getter
    @Setter
    public static class TaskTemplateItemResponse {
        private String taskTemplateId;
        private String checklistTemplateId;
        private String name;
        private String description;
        private Integer orderNo;
        private String status;
    }
}
