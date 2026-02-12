package com.sme.be_sme.modules.company.api.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CompanyRegisterResponse {
    private String companyId;
    private String adminUserId;
    private String accessToken;
    private String tokenType;
    private Long expiresInSeconds;
}
