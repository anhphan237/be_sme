package com.sme.be_sme.modules.onboarding.api.response;

import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TaskLibraryListResponse {
    private List<TaskLibraryItem> items;

    @Getter
    @Setter
    public static class TaskLibraryItem {
        private String templateId;
        private String name;
        private String status;
        private String departmentTypeCode;
        private String departmentTypeName;
    }
}
