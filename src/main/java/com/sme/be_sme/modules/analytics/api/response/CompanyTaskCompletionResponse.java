package com.sme.be_sme.modules.analytics.api.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CompanyTaskCompletionResponse {
    private String companyId;
    private int totalTasks;
    private int completedTasks;
    private double completionRate;
}
