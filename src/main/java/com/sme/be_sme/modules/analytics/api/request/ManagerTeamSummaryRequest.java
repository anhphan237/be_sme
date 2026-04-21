package com.sme.be_sme.modules.analytics.api.request;

import lombok.Data;

@Data
public class ManagerTeamSummaryRequest {
    private String companyId;
    private String managerUserId;
    private String startDate;
    private String endDate;
}