package com.sme.be_sme.modules.platform.api.response;

import lombok.Data;

@Data
public class PlatformCompanyAnalyticsResponse {
    private Integer totalCompanies;
    private Integer activeCompanies;
    private Integer inactiveCompanies;
    private Integer suspendedCompanies;
    private Integer newCompanies;
    private Double growthRate;
}
