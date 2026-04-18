package com.sme.be_sme.modules.analytics.api.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CompanyOnboardingTemplateScoreboardRequest {
    private String companyId;
    private String templateId;
    /** Optional; defaults to ACTIVE */
    private String status;
    /** Optional; defaults to 20 */
    private Integer limit;
}
