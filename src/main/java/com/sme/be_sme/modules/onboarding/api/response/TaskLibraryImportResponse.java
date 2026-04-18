package com.sme.be_sme.modules.onboarding.api.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TaskLibraryImportResponse {
    private String templateId;
    private String departmentTypeCode;
    private boolean created;
    private int totalRows;
    private int importedTasks;
}
