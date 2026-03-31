package com.sme.be_sme.modules.onboarding.api.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TaskListByAssigneeRequest {

    /** Optional filter (TODO, IN_PROGRESS, DONE, …). */
    private String status;
    private Integer page;
    private Integer size;
    private String sortBy;
    private String sortOrder;
}
