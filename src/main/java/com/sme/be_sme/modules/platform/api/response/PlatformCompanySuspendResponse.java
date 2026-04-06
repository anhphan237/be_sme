package com.sme.be_sme.modules.platform.api.response;

import lombok.Data;

@Data
public class PlatformCompanySuspendResponse {
    private String companyId;
    private String status;
    private String message;
}
