package com.sme.be_sme.modules.onboarding.api.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TaskListByOnboardingRequest {
    private String onboardingId;      // Required - ID của onboarding instance
    private String status;            // Optional - Filter: TODO, IN_PROGRESS, DONE
    private String assignedUserId;    // Optional - Filter theo user được gán
    private String sortBy;            // Optional - due_date, created_at, status
    private String sortOrder;         // Optional - ASC, DESC (default: ASC)
    private Integer page;             // Optional - Pagination (default: 1)
    private Integer size;             // Optional - Page size (default: 20)
}
